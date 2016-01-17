package reson.server

import com.twitter.finagle.http.Method._
import com.twitter.finagle.http.{HeaderMap, Request}
import rapture.data.GeneralExtractors
import rapture.json._
import rapture.json.jsonBackends.argonaut._
import reson.ParameterParser._
import reson._
import reson.server.Query.{DataRow, Ops}

import scala.collection.immutable.Iterable
import scala.util.{Failure, Try}


/**
  * Created by sscarduzio on 11/01/2016.
  */

//////// ADT
trait Query {
  val table: String
  def materialize: Try[String]
}

trait Where {
  val where: Iterable[_ <: Op]
  lazy val whereString = if (where.isEmpty) "" else where.map(_.toString).mkString("WHERE 1 AND ", " AND ", "")
}

case class UpdateQ(table: String, rows: List[DataRow], where: Ops) extends Query with Where {
  def materialize = {
    for {
      r <- Option(rows).filter(!_.isEmpty).map(Try(_)).getOrElse(Failure(new Exception("No rows provided"))) // SHOULD return immediately if rows is empty
      rowList <- Try(rows).filter(_.size == 1).orElse(Failure(new Exception("You should provide just one object")))
      singleRow <- rowList.headOption.map(Try(_)).getOrElse(Failure(new Exception)) // Never fails
      fieldsEqValues = singleRow.map(kv => s"${kv._1}='${kv._2}'")
    } yield
      s"""UPDATE $table SET ${fieldsEqValues.mkString(", ")} $whereString"""
  }
}

case class InsertQ(table: String, rows: List[DataRow]) extends Query {

  import Query._

  def materialize = {
    val opt = for {
      (fields, values) <- toFieldsAndValues(rows)
    } yield s"INSERT INTO $table " + fields.mkString("(", ",", ")") + " VALUES " + values.map(_.map(fixQuotes).mkString("(", ",", ") ")).mkString(",")
    opt.map(Try(_)).getOrElse(Failure(new Exception("cannot build Insert query")))
  }

}

case class DeleteQ(table: String, where: Ops) extends Query with Where {
  def materialize = {
    val opt = Option(where).filter(!_.isEmpty).map(wh => s"""DELETE FROM $table ${whereString}""")
    opt.map(Try(_)).getOrElse(Failure(new Exception("cannot build Delete query")))
  }
}

case class SelectQ(table: String, select: String, where: Ops, order: Option[Order], limit: Option[(Int, Int)]) extends Query with Where {
  def materialize = Try {
    s"""SELECT $select from $table ${whereString} $formatOrder $formatLimit"""
  }

  def formatOrder = order.map(_.toString).getOrElse("")

  def formatLimit = limit.map(r => s"LIMIT ${r._1}, ${r._2}").getOrElse("")
}

object Query {
  type Ops = List[_ <: Op]
  type DataRow = Map[String, String]

  def toFieldsAndValues(rows: List[DataRow]): Option[(Iterable[String], List[Iterable[String]])] = {
    for {
      aRow <- rows.headOption
      fields = aRow.map(_._1)
      values = rows.map(_.map(_._2))
    } yield (fields, values)
  }

  val mkWhereOps: Request => String = req => {
    val whereOps: Set[_ <: Op] = {
      val wherePars = req.params.filter(s => (s != "select" && s != "order"))
      val ops = wherePars.map(parseParam)
      Option(ops.toSet).filter(!_.isEmpty).getOrElse(Set())
    }
    if (whereOps.isEmpty) "" else whereOps.map(_.toString).mkString("WHERE 1 AND ", " AND ", "")
  }

  val fixQuotes: String => String = v => "'" + v + "'"


}

object RequestParser {

  implicit def me: Extractor[Map[String, Json], Json] = GeneralExtractors.mapExtractor

  def parse(req: Request, table: String): Try[Query] = {

    lazy val rows: List[Map[String, String]] = {
      val jo: Json = Json.parse(req.contentString)
      val jRows = if (jo.is[List[Json]]) jo.as[List[Json]] else List(jo.as[Json])
      jRows.map(_.as[Map[String, Json]]
        // When a Json is a string type, it's toString method returns the field in double quotes, unless we extract it properly as string
        .map(kv => kv._1 -> (if(kv._2.is[String]) kv._2.as[String] else kv._2.toString)))
    }

    lazy val paramOps: Ops = {
      val groupedByPriority = req.params.map(parseParam).groupBy {
        _ match {
          case s: Select => 1
          case s: Order => 2
          case _ => 3
        }
      }
      groupedByPriority.keySet.foldLeft(List[Op]())((acc, v) => acc ++ groupedByPriority(v))
    }

    (req.method) match {
      case Get => paramOps match {
        case (s: Select) :: (o: Order) :: rest => Try(SelectQ(table, s.value, rest, Option(o), rangeSuffix(req.headerMap)))
        case (s: Select) :: rest => Try(SelectQ(table, s.value, rest, None, rangeSuffix(req.headerMap)))
        case (o: Order) :: rest => Try(SelectQ(table, "*", rest, Option(o), rangeSuffix(req.headerMap)))
        case rest => Try(SelectQ(table, "*", rest, None, rangeSuffix(req.headerMap)))
      }
      case Post => Try(InsertQ(table, rows))
      case Patch => Try(UpdateQ(table, rows, paramOps))
      case _ => Failure(new Exception("Cannot parse request"))
    }
  }

  def rangeSuffix(hmap: HeaderMap): Option[(Int, Int)] = {
    def mkRange(s: String): (Int, Int) = {
      lazy val error = new Exception("HTTP Range error")
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

    hmap.get("Range").map(mkRange)
  }
}

