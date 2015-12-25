package reson.db

import com.twitter.conversions.time._
import com.twitter.finagle.client.DefaultPool
import com.twitter.finagle.exp.Mysql
import com.twitter.finagle.http.HeaderMap
import com.twitter.util.Future
import rapture.json._
import rapture.json.jsonBackends.argonaut._
import reson.RequestNotSatisfiable

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
    val jbListF =
      db.prepare(
        """SELECT TABLE_SCHEMA as `schema`, TABLE_NAME as name, TABLE_TYPE='BASE TABLE' as insertable
          |FROM information_schema.tables
          |WHERE TABLE_SCHEMA = ? """.stripMargin)
        .select(dbConf.dbName)(Json(_))
    jbListF.map(lj => Json(lj).toString)
  }

  def getTable(t: String, hmap: HeaderMap): Future[String] = {
    if (!t.matches("^[a-zA-Z0-9_-]*$")) throw new Exception("no good table name")
    def mkRange(s: String): (Int, Int) = {
      lazy val error = new RequestNotSatisfiable("HTTP Range error")
      Try {
        Option(s).filter(_.matches("^(-)?([0-9]+)-(-)?([0-9]*)$")).map { _ =>
          Option(s.takeWhile(_ != '-')).filter(!_.isEmpty) match {
            case Some(fst) => (fst.toInt, (s.drop(fst.length + 1).toInt))
            case None => {
              val fst = '-' + s.tail.takeWhile(_ != '-')
              (fst.toInt, s.drop(fst.length + 1).toInt)
            }
          }
        }.filter(t => t._1  < t._2).getOrElse(throw error) // range length should be > 0 to be meaningful
      }.getOrElse(throw error) // We had the header, but no number could be parsed.
    }

    val range = hmap.get("Range").map(mkRange).getOrElse((0,-1))
    db.prepare(s"select * from $t limit ${range._1} ${range._2}").select(t)(Json(_))
      .map(q => Json(q).toString)
  }

}
