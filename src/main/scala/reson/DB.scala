package reson

import com.twitter.finagle.client.DefaultPool
import com.twitter.finagle.exp.Mysql
import com.twitter.finagle.exp.mysql.Row
import com.twitter.util.Future
import reson.tonno.api.MySQL2Json
import com.twitter.conversions.time._

import rapture.json._
import rapture.json.jsonBackends.argonaut._

/**
  * Created by sscarduzio on 23/12/2015.
  */
object DB extends MySQL2Json {

  val db = Mysql.client
    .withCredentials(sys.env("db_user"), sys.env.get("db_pass").getOrElse(null))
    .withDatabase(sys.env("db_name"))
    .configured(DefaultPool.Param(
      low = 1, high = 10,
      idleTime = 5.minutes,
      bufferSize = 0,
      maxWaiters = Int.MaxValue))
    .newRichClient(sys.env("db_host"))


  def getTableList: Future[String] = {
    val jbListF =
      db.prepare(
        """SELECT TABLE_SCHEMA as `schema`, TABLE_NAME as name, TABLE_TYPE='BASE TABLE' as insertable
          |FROM information_schema.tables
          |WHERE TABLE_SCHEMA = ? """.stripMargin)
        .select(sys.env("db_name"))(Json(_))
    jbListF.map(lj => Json(lj).toString)
  }

  def getTable(t: String): Future[String] = {
    if(!t.matches("^[a-zA-Z0-9_-]*$")) throw new Exception("no good table name")
    db.prepare(s"select * from $t ").select(t)(Json(_))
      .map(q => Json(q).toString)
  }

}
