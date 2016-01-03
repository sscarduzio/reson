package reson

import com.twitter.finagle.http.Method._
import com.twitter.finagle.http._
import com.twitter.finagle.http.path._
import com.twitter.finagle.{Http, Service}
import com.twitter.util.{Await, Future}
import reson.db.MySQL
import reson.server.Req2Query

/**
  * Created by sscarduzio on 23/12/2015.
  */
object Server extends App {
  lazy val route = toService { req =>
    (req.method, Path(req.path)) match {
      case (Get, Root) => MySQL.getTableList.map(mkResp)
      case (Get, Root / (table: String)) => MySQL.read(Req2Query.parseReadRequest(req, table)).map(mkResp)
      case (Post, Root / (table:String)) => MySQL.write(Req2Query.parseInsertRequest(req,table)).map(mkResp)
      case (Patch, Root / (table:String)) => MySQL.write(Req2Query.parseUpdateRequest(req,table)).map(mkResp)
    }
  }

  val q = Await.ready(Http.serve(":5000", exceptionHandlerFilter andThen new Service[Request, Response] {
    def apply(req: Request): Future[Response] = route(req)
  }))
  println("listening: " + q)
}
