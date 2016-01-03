/**
  * Created by sscarduzio on 25/12/2015.
  */
package reson

import com.twitter.finagle.http.Method.Get
import com.twitter.finagle.http.{Request, Status}
import com.twitter.util.Await
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.{BeforeAndAfterEach, FunSuite}
import rapture.json._
import rapture.json.jsonBackends.argonaut._

@RunWith(classOf[JUnitRunner])
class ServerTest extends FunSuite with BeforeAndAfterEach {
  sys.props("db_uri") = "mysql://root:@localhost:3306/reson_it"

  test("GET Ok") {
    val responseFuture = Server.route(Request(Get, "/"))
    val response = Await.result(responseFuture)

    assert(response.status === Status.Ok)
    assert(Json.parse(response.contentString).as[List[Json]].headOption.map(_.schema.as[String] == "reson_it").getOrElse(false))
  }
}