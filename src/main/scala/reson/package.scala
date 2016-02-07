import com.twitter.finagle.http.{Status, Response}

import rapture.json._
import rapture.json.jsonBackends.jackson._

/**
  * Created by sscarduzio on 23/12/2015.
  */
package object reson {

  def mkResp(str:String ) = {
    val resp = Response(Status.Ok)
    resp.setContentTypeJson
    resp.contentString = Option(str).filter(!_.isEmpty).getOrElse("{}")
    resp
  }

  def CANNED(s:Status, msg:Json) = {
    val j = json"""{ "status": ${s.reason}, "message": $msg}"""
    mkResp(j.toString)
  }

  def CANNED(s: Status, msg: String) = {
    val j = json"""{ "status": ${s.reason}, "message": $msg}"""
    mkResp(j.toString)
  }
}
