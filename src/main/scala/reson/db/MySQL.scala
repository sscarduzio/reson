package reson.db

import com.twitter.finagle.Mysql
import com.twitter.finagle.client.DefaultPool
import com.twitter.finagle.mysql.{Client, OK, Transactions}
import com.twitter.util._
import org.json4s.JsonDSL._
import org.json4s._

/**
  * Created by sscarduzio on 23/12/2015.
  */

object MySQL extends MySQL2Json {

  val dbConf: ConnectionConfig = ConnectionConfig(sys.props.get("db_uri").getOrElse(sys.env("db_uri")))

  lazy val db: Client with Transactions = Mysql.client
    .withCredentials(dbConf.user, dbConf.pass.orNull)
    .withDatabase(dbConf.dbName)
    .configured(DefaultPool.Param(
      low = 1, high = 10,
      idleTime = Duration.fromMinutes(5),
      bufferSize = 0,
      maxWaiters = Int.MaxValue))
    .newRichClient(dbConf.hostAndPort.toString)

  def tableList: Future[JValue] = {
    // Shitty bug in MySQL derived column return type
    def fixBigintToBool(j: JObject): JObject = JObject(j.obj map {
      case ("insertable", JDecimal(n)) => "insertable" -> JBool(n != 0)
      case ("insertable", JInt(n)) => "insertable" -> JBool(n != 0)
      case ("insertable", JLong(n)) => "insertable" -> JBool(n != 0)
      case (k, v) => k -> v
    })
    val jbListF: Future[Seq[JObject]] =
      db.prepare(
        """SELECT TABLE_SCHEMA as `schema`, TABLE_NAME as name, TABLE_TYPE = 'BASE TABLE' as insertable
          |FROM information_schema.tables
          |WHERE TABLE_SCHEMA = ? """.stripMargin)
        .select(dbConf.dbName)(mysqlRowToJsonSerializer)
        .map(_.map(fixBigintToBool))
    jbListF.map(l => JArray(l.toList))
  }

  def read(query: String): Future[JValue] = {
    println(s"READ: $query")
    db.select(query)(mysqlRowToJsonSerializer).map(q => JArray(q.toList))
  }

  def write(query: String): Future[JValue] = {
    println(s"WRITE: $query")
    db.query(query).flatMap {
      case r: OK =>
        Future.value(("status" -> "OK") ~ ("affected_rows" -> r.affectedRows))
      case err => Future.exception(new Exception(err.toString))
    }
  }

}
