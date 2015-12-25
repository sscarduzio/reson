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

@RunWith(classOf[JUnitRunner])
class ServerTest extends FunSuite with BeforeAndAfterEach {
  sys.props("db_uri") = "mysql://root:@localhost:3306/tonno"
  test("GET Ok") {
    val responseFuture = Server.route(Request(Get, "/"))
    val response = Await.result(responseFuture)

    assert(response.status === Status.Ok)
  }
}