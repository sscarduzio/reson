import com.twitter.finagle.http.{Response, Status}
import org.json4s.JsonDSL._
import org.json4s._
import org.json4s.jackson.JsonMethods._

/**
  * Created by sscarduzio on 23/12/2015.
  */
package object reson {

  def mkResp(str: String): Response = {
    val resp = Response(Status.Ok)
    resp.setContentTypeJson()
    resp.contentString = Option(str).filter(!_.isEmpty).getOrElse("{}")
    resp
  }

  def mkResp(payload: JValue): Response = {
    mkResp(compact(payload))
  }

  def CANNED(s: Status, msg: JValue): Response =
    mkResp(("status" -> s.reason) ~ ("message" -> msg))

  def CANNED(s: Status, msg: String): Response =
    CANNED(s, JString(msg))

}
