package reson

import com.twitter.finagle.http.Method._
import com.twitter.finagle.http._
import com.twitter.finagle.http.path._
import com.twitter.finagle.{Http, Service}
import com.twitter.util.{Await, Future}
import reson.db.MySQL

/**
  * Created by sscarduzio on 23/12/2015.
  */
object Server extends App {
  lazy val route = toService { req =>
    (req.method, Path(req.path)) match {
      case (Get, Root) => MySQL.getTableList.map(mkResp)
      case (Get, Root / (table: String)) => MySQL.getTable(table, req.headerMap).map(mkResp)
    }
  }

  Await.ready(Http.serve(":8080", new Service[Request, Response] {
    def apply(req: Request): Future[Response] = route(req)
  }))
}