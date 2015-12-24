import com.twitter.finagle.Service
import com.twitter.finagle.http.{Status, Response, Request}
import com.twitter.util.Future

/**
  * Created by sscarduzio on 23/12/2015.
  */
package object reson {
  def toService(f: Request => Future[Response]): Service[Request, Response] = {
    new Service[Request, Response] {
      def apply(req: Request): Future[Response] = f(req)
    }
  }

  val mkResp: String => Response = { str =>
    val resp = Response(Status.Ok)
    resp.setContentTypeJson
    resp.contentString = str
    resp
  }

}
