/**
  * Created by sscarduzio on 25/12/2015.
  */
package reson

import com.twitter.finagle.exp.mysql.{Result, Error, OK}
import com.twitter.finagle.http.Method.Get
import com.twitter.finagle.http.{Request, Status}
import com.twitter.util.{Future, Duration, Await}
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.{BeforeAndAfterEach, FunSuite}
import reson.db.MySQL
import rapture.json._
import rapture.json.jsonBackends.argonaut._

import scala.util.Try

@RunWith(classOf[JUnitRunner])
class ServerTest extends FunSuite with BeforeAndAfterEach {
  sys.props("db_uri") = "mysql://root:@localhost:3306/reson_it"
  val ddl = """DROP TABLE IF EXISTS `order_items`;
              |
              |CREATE TABLE `order_items` (
              |  `order_id` int(11) NOT NULL,
              |  `item_id` int(11) NOT NULL,
              |  `float` float NOT NULL,
              |  `double` double DEFAULT NULL,
              |  `boolean` tinyint(4) DEFAULT NULL,
              |  `bigint` bigint(20) DEFAULT NULL,
              |  `decimal` decimal(10,2) DEFAULT NULL,
              |  `unsignedint` int(11) unsigned DEFAULT NULL,
              |  `varchar` varchar(44) DEFAULT NULL,
              |  `date` date DEFAULT NULL,
              |  `datetime` datetime DEFAULT NULL,
              |  `timestamp` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
              |  `year` year(4) DEFAULT NULL,
              |  PRIMARY KEY (`order_id`,`item_id`)
              |) ENGINE=InnoDB DEFAULT CHARSET=utf8;
              |
              |
              |INSERT INTO `order_items` (`order_id`, `item_id`, `float`, `double`, `boolean`, `bigint`, `decimal`, `unsignedint`, `varchar`, `date`, `datetime`, `timestamp`, `year`)
              |VALUES
              |	(1,1,2.3,2.22,1,93939393,999.22,99,'lolol','2015-12-25','2015-12-25 23:11:33','2015-12-25 21:11:11','2016');
              |
              |DROP TABLE IF EXISTS `orders`;
              |
              |CREATE TABLE `orders` (
              |  `id` int(11) NOT NULL AUTO_INCREMENT,
              |  `ref` varchar(255) NOT NULL,
              |  PRIMARY KEY (`id`)
              |) ENGINE=InnoDB DEFAULT CHARSET=utf8;
              |
              |INSERT INTO `orders` (`id`, `ref`)
              |VALUES
              |	(1,'RR34567890');
              |
              |""".stripMargin
  val lf: List[Future[Result]] = ddl.split(";").map(_.trim + ";").toList
    .map(MySQL.db.query(_).rescue{case e => e.printStackTrace; Future(Error(1,"oops","meh"))})

      val aw = lf.map(Await.result(_, Duration.Top))


  test("GET Ok") {
    val responseFuture = Server.route(Request(Get, "/"))
    val response = Await.result(responseFuture)

    assert(response.status === Status.Ok)
    assert(Json.parse(response.contentString).as[List[Json]].headOption.map(_.schema.as[String] == "reson_it").getOrElse(false))
  }
}