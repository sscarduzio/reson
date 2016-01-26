package reson

import com.twitter.finagle.http.Method._
import com.twitter.finagle.http._
import com.twitter.finagle.http.Status._
import com.twitter.finagle.http.path._
import com.twitter.finagle.{SimpleFilter, Http, Service}
import com.twitter.util.{Await, Future}
import reson.db.MySQL
import reson.server.{RequestParser}
import util.Transformers._

/**
  * Created by sscarduzio on 23/12/2015.
  */
object Server extends App {
  def handle(req: Request, table: String, f: String => Future[String]): Future[Response] = {
    for {
      query <- RequestParser.parse(req, table).toFuture
      qString <- query.materialize.toFuture
      dbResp <- f(qString)
    } yield mkResp(dbResp)
  }

  lazy val route = Service.mk[Request, Response] { req =>
    (req.method, Path(req.path)) match {
      case (Get, Root) => MySQL.tableList.map(mkResp)
      case (Get, Root / (table: String)) => handle(req, table, MySQL.read)
      case (_, Root / (table: String)) => handle(req, table, MySQL.write)
      case _ => Future(CANNED(NotFound, "Route not found"))
    }
  }

 lazy val exceptionHandlerFilter = new SimpleFilter[Request, Response] {
    def apply(req: Request, service: Service[Request, Response]) = service(req) handle {
      case e => {
        println(e.getMessage)
        e.printStackTrace
        if (Option(e.getMessage).filter(!_.isEmpty).isDefined) CANNED(InternalServerError, e.getMessage) else CANNED(InternalServerError, e.getStackTrace.toString)
      }
    }
  }

  lazy val svc = exceptionHandlerFilter andThen new Service[Request, Response] {
    def apply(req: Request): Future[Response] = route(req)
  }
  val q = Await.ready(Http.serve(":5000", svc))
  println("listening: " + q)
}
