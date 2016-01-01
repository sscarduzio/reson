package reson.db

import com.twitter.conversions.time._
import com.twitter.finagle.client.DefaultPool
import com.twitter.finagle.exp.Mysql
import com.twitter.finagle.http.HeaderMap
import com.twitter.util.Future
import rapture.json._
import rapture.json.jsonBackends.argonaut._
import reson.RequestNotSatisfiable
import reson.server.Req2Query

import scala.util.Try

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

  def getTableList: Future[String] = {
    // Shitty bug in MySQL derived column return type
    def fixBigintToBool(j: Json): Json = {
      val jb = JsonBuffer(j)
      jb.insertable = j.insertable.as[Int] == 1
      Json(jb)
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

  def getTable(t: String, hmap: HeaderMap): Future[String] = {
    val rangeSuffix = Req2Query.rangeSuffix(hmap)
    db.prepare(s"SELECT * FROM $t $rangeSuffix").select(t)(Json(_))
      .map(q => Json(q).toString)
  }

  def read(query:String):Future[String] = {
    db.select(query)(Json(_)).map(q => Json(q).toString)
  }
}
