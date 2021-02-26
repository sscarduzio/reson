package reson

import com.twitter.finagle.http.Method.{Get, Options}
import com.twitter.finagle.http.Status.{Conflict, InternalServerError, NotFound}
import com.twitter.finagle.http.path.{/, Path, Root}
import com.twitter.finagle.http.{Request, Response}
import com.twitter.finagle.mysql.ServerError
import com.twitter.finagle.{Http, Service, SimpleFilter}
import com.twitter.util.{Await, Future}
import org.json4s.JsonAST.JValue
import org.json4s.JsonDSL._
import reson.db.MySQL
import reson.server.RequestParser
import reson.util.Transformers._

/**
 * Created by sscarduzio on 23/12/2015.
 */
object Server extends App {
  def handle(req: Request, table: String, f: String => Future[JValue]): Future[Response] = {
    for {
      query <- RequestParser.parse(req, table).toFuture
      qString <- query.materialize.toFuture
      dbResp <- f(qString)
    } yield mkResp(dbResp)
  }

  lazy val route = Service.mk[Request, Response] { req =>
    (req.method, Path(req.path)) match {
      case (Get, Root) => MySQL.tableList.map(mkResp)
      case (Get | Options, Root / (table: String)) => handle(req, table, MySQL.read)
      case (_, Root / (table: String)) => handle(req, table, MySQL.write)
      case _ => Future(CANNED(NotFound, "Route not found"))
    }
  }

  lazy val exceptionHandlerFilter = new SimpleFilter[Request, Response] {
    def apply(req: Request, service: Service[Request, Response]): Future[Response] = {

      service(req) handle {

        // DUP primary key or DUP for UNIQUE column
        case e: ServerError if e.code == 1062 || e.code == 1169 =>
          CANNED(Conflict, ("code" -> e.code) ~ ("message" -> e.message) ~ ("details" -> e.sqlState) ~ ("hint" -> null))

        // Other SQL errors
        case e: ServerError =>
          CANNED(InternalServerError, ("code" -> e.code) ~ ("message" -> e.message) ~ ("details" -> e.sqlState) ~ ("hint" -> null))

        // Reson shat the bed
        case e =>
          sys.error("ERROR: " + e.getMessage)
          e.printStackTrace()
          CANNED(InternalServerError, e.getMessage)
      }
    }
  }

  lazy val svc = exceptionHandlerFilter andThen new Service[Request, Response] {
    def apply(req: Request): Future[Response] = route(req)
  }
  val q = Await.ready(Http.serve(":5000", svc))
  println("listening: " + q)
}
