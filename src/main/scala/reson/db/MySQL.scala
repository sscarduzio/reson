package reson.db

import com.twitter.conversions.time._
import com.twitter.finagle.client.DefaultPool
import com.twitter.finagle.exp.Mysql
import com.twitter.finagle.exp.mysql.{OK, Error}
import com.twitter.util.Future
import rapture.json._
import rapture.json.jsonBackends.jackson._

/**
  * Created by sscarduzio on 23/12/2015.
  */

object MySQL extends MySQL2Json {

  val dbConf = ConnectionConfig(sys.props.get("db_uri").getOrElse(sys.env("db_uri")))

  lazy val db = Mysql.client
    .withCredentials(dbConf.user, dbConf.pass.getOrElse(null))
    .withDatabase(dbConf.dbName)
    .configured(DefaultPool.Param(
      low = 1, high = 10,
      idleTime = 5.minutes,
      bufferSize = 0,
      maxWaiters = Int.MaxValue))
    .newRichClient(dbConf.hostAndPort.toString)

  def tableList: Future[String] = {
    // Shitty bug in MySQL derived column return type
    def fixBigintToBool(j: Json): Json = {
      val isInsertable =  j.insertable.as[Int] == 1
      j ++ json"""{"insertable": $isInsertable}"""
    }
    val jbListF: Future[Seq[Json]] =
      db.prepare(
        """SELECT TABLE_SCHEMA as `schema`, TABLE_NAME as name, TABLE_TYPE = 'BASE TABLE' as insertable
          |FROM information_schema.tables
          |WHERE TABLE_SCHEMA = ? """.stripMargin)
        .select(dbConf.dbName)(Json(_))
        .map(_.map(fixBigintToBool))
    jbListF.map(lj => Json(lj).toString)
  }

  def read(query: String): Future[String] = {
    println(s"READ: $query")
    db.select(query)(Json(_)).map(q => Json(q).toString)
  }

  def write(query: String): Future[String] = {
    println(s"WRITE: $query")
    db.query(query).flatMap {
      _ match {
        case r: OK => Future.value(json"""{ "status": "OK","affected_rows": ${r.affectedRows}}""".toString)
        case e: Error => Future.exception[String](new Exception( json"""{"code": ${e.code}, "message": ${e.message}, "details": ${e.sqlState}, "hint": null }""".toString))
        case err => Future.exception(new Exception(err.toString))
      }
    }
  }

}
