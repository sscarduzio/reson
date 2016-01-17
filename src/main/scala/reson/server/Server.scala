package reson

import com.twitter.finagle.http.Method._
import com.twitter.finagle.http._
import com.twitter.finagle.http.path._
import com.twitter.finagle.{Http, Service}
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
      query <- RequestParser.parse(req, table).future
      qString <- query.materialize.future
      dbResp <- f(qString)
    } yield mkResp(dbResp)
  }

  lazy val route = Service.mk[Request, Response] { req =>
    (req.method, Path(req.path)) match {
      case (Get, Root) => MySQL.tableList.map(mkResp)
      case (Get, Root / (table: String)) => handle(req, table, MySQL.read)
      case (_, Root / (table: String)) => handle(req, table, MySQL.write)
    }
  }

  val q = Await.ready(Http.serve(":5000", exceptionHandlerFilter andThen new Service[Request, Response] {
    def apply(req: Request): Future[Response] = route(req)
  }))
  println("listening: " + q)
}
