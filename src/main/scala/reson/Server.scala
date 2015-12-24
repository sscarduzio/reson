package reson

import com.twitter.finagle.http.service.RoutingService
import com.twitter.finagle.{Http, Service}
import com.twitter.finagle.http._
import com.twitter.util.{Await, Future}
import rapture.json._
import rapture.json.jsonBackends.argonaut._
import com.twitter.finagle.http.path._

/**
  * Created by sscarduzio on 23/12/2015.
  */
object Server extends App {

  val backend = RoutingService.byPathObject {
    case Root => toService { _ =>
      DB.getTableList.map(mkResp)
    }
    case Root /(table:String) => toService { _ =>
      DB.getTable(table).map(mkResp)
    }
  }

  val server = Http.serve(":8080", backend)
  Await.ready(server)
}
