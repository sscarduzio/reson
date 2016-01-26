/**
  * Created by sscarduzio on 24/01/2016.
  */

package reson.server

import java.io.File

import com.twitter.finagle.http.Method.Get
import com.twitter.finagle.http.Request
import org.specs2._
import org.specs2.concurrent.ExecutionEnv
import org.specs2.matcher.{ExpectationsDescription, FutureMatchers}
import org.specs2.specification.BeforeEach
import reson.Server
import reson.util.TwitterConverters._

import scala.concurrent.duration._
import scala.sys.process._

import rapture.json._
import rapture.json.jsonBackends.jackson._


class SelectSpecs extends mutable.Specification with BeforeEach with FutureMatchers with ExpectationsDescription {

  def before = {
    println("Importing the test schema SQL dump..")
    "mysql -u root reson_it " #< new File("reson_it.sql")

    println("Adding DB configuration to system properties..")
    sys.props("db_uri") = "mysql://root:@localhost/reson_it"

    println("Setup done!")
  }

  "When listing tables, Reson should return all the tables with a boolean insertable field" >> { implicit ee: ExecutionEnv =>

    Server.svc(Request(Get, "/")).toScala.map(resp => Json.parse(resp.contentString)) must be_==(
      json"""
      [
        {"schema": "reson_it", "name": "order_items", "insertable": "true"},
        {"schema": "reson_it","name": "orders","insertable": "true"},
        {"schema": "reson_it","name": "orders_view","insertable": "false"}
      ]
      """
    ).await(retries = 2, timeout = 1.seconds)

  }


}