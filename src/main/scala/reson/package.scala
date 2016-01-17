import com.twitter.finagle.{SimpleFilter, Service}
import com.twitter.finagle.http.{Status, Response, Request}
import com.twitter.util.Future
import com.twitter.finagle.http.Status._

import rapture.json._
import rapture.json.jsonBackends.argonaut._

/**
  * Created by sscarduzio on 23/12/2015.
  */
package object reson {

  val mkResp: String => Response = { str =>
    val resp = Response(Status.Ok)
    resp.setContentTypeJson
    resp.contentString = Option(str).filter(!_.isEmpty).getOrElse("{}")
    resp
  }

  def CANNED(s: Status, msg: String) = {
    mkResp( json"""{ "status": $s, "message": $msg}""".toString)
  }

  val exceptionHandlerFilter = new SimpleFilter[Request, Response] {
    def apply(req: Request, service: Service[Request, Response]) = service(req) handle {
      case e => {
        println(e.getMessage)
        e.printStackTrace
        if (Option(e.getMessage).filter(!_.isEmpty).isDefined) CANNED(InternalServerError, e.getMessage) else CANNED(InternalServerError, e.getStackTraceString)
      }
    }
  }
}
