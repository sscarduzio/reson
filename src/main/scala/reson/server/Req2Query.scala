package reson.server

import argonaut.Json
import argonaut._, Argonaut._

import com.twitter.finagle.http.{HeaderMap, Request}
import reson.Param2Op._
import reson.{Op, NoOp, RequestNotSatisfiable}
import rapture.json.jsonBackends.argonaut._

import scala.util.Try


/**
  * Created by sscarduzio on 01/01/2016.
  */
object Req2Query {

  val fixQuotes: Json => String = v => "'" + v.toString.drop(1).dropRight(1) + "'"
  def parseInsertRequest(req: Request, table: String): String = {
    val (fields, values) = parseBodyObject(req,table)
    val query = s"INSERT INTO $table " + fields.mkString("(", ",",")") + " VALUES " + values.map(fixQuotes).mkString("(", ",",");")
    println(query)
    query
  }

  def parseUpdateRequest(req: Request, table: String): String = {
    val (fields, values) = parseBodyObject(req,table)
    val assignments = fields.zip(values).map(e => s"${e._1}=${fixQuotes(e._2)}").mkString(",")
    val query = s"""UPDATE $table SET  $assignments ${mkWhereOps(req)} ;"""
    println(query)
    query
  }

  private def parseBodyObject(req: Request, table: String): (List[Json.JsonField], List[Json]) = {
    val fieldsAndValuesO = {
      for {
        jo <- argonaut.Parse.parse(req.contentString)
        flds = jo.hcursor.fields.getOrElse(throw new Exception("no fields"))
        vls = jo.objectValues.getOrElse(throw new Exception("no values"))
      } yield (flds, vls)
    }.toOption

    fieldsAndValuesO.getOrElse(throw new Exception("impossible to parse JSON"))
  }

  val mkWhereOps : Request => String = req =>  {
    val whereOps: Set[_ <: Op] = {
      val wherePars = req.params.filter(s => (s != "select" && s != "order"))
      val ops = wherePars.map(parseParam)
      Option(ops.toSet).filter(!_.isEmpty).getOrElse(Set())
    }
    if(whereOps.isEmpty) "" else whereOps.map(_.toString).mkString("WHERE 1 AND ", " AND ", "")
  }

  def parseReadRequest(req: Request, table: String): String = {
    val params = req.params
    val select = parseParam("select", params.get("select").getOrElse("*"))
    val order = params.get("order").map(parseParam("order", _)).getOrElse(NoOp)

    val qs: Set[String] =
      Set(
        select.toString + s" from $table",
        mkWhereOps(req),
        rangeSuffix(req.headerMap),
        order.toString
      )
    val q = qs.mkString("", " ", ";")
    println(q)
    q
  }

  def rangeSuffix(hmap: HeaderMap) = {
    def mkRange(s: String): (Int, Int) = {
      lazy val error = new RequestNotSatisfiable("HTTP Range error")
      val parseAttempt = Try {

        val wellFormed = Option(s).filter(_.matches("^(-)?([0-9]+)-(-)?([0-9]*)$"))

        val parsed = wellFormed.map { _ =>
          Option(s.takeWhile(_ != '-')).filter(!_.isEmpty) match {
            case Some(fst) => (fst.toInt, (s.drop(fst.length + 1).toInt))
            case None => {
              val fst = '-' + s.tail.takeWhile(_ != '-')
              (fst.toInt, s.drop(fst.length + 1).toInt)
            }
          }
        }
        parsed.filter(t => t._1 < t._2).getOrElse(throw error) // range length should be > 0 to be meaningful
      }
      parseAttempt.getOrElse(throw error) // We had the header, but no number could be parsed.
    }

    val range = hmap.get("Range").map(mkRange)
    range.map(r => s"LIMIT ${r._1}, ${r._2}").getOrElse("")
  }
}
