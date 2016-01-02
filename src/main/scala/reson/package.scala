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
  def toService(f: Request => Future[Response]): Service[Request, Response] = {
    new Service[Request, Response] {
      def apply(req: Request): Future[Response] = f(req)
    }
  }

  final case class RequestNotSatisfiable(msg:String) extends Exception

  val mkResp: String => Response = { str =>
    val resp = Response(Status.Ok)
    resp.setContentTypeJson
    resp.contentString = str
    resp
  }


  final case class AuthorizationFailure(msg:String) extends Exception

  val exceptionHandlerFilter = new SimpleFilter[Request, Response] {
    def apply(req: Request, service: Service[Request, Response]) = service(req) handle {
      case af: AuthorizationFailure => {
        val res = Response(Unauthorized)
        res.setContentTypeJson
        val cleanedMessage = json"""{"status": "Unauthorized", "cause": ${af.getMessage} }""".toString
        res.setContentString(cleanedMessage)
        af.printStackTrace
        res
      }
      case e => {
        println(e.getMessage)
        e.printStackTrace
        val res = Response(InternalServerError)
        if (Option(e.getMessage).filter(!_.isEmpty).isDefined) res.setContentString(e.getMessage) else res.setContentString(e.getStackTraceString)
        res
      }
    }
  }

}
