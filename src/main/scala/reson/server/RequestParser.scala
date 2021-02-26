package reson.server

import com.twitter.finagle.http.Method._
import com.twitter.finagle.http.{HeaderMap, Request}
import org.json4s.JsonAST._
import org.json4s.jackson.JsonMethods
import org.json4s.jackson.JsonMethods._
import reson.ParameterParser._
import reson._
import reson.db.MySQL
import reson.server.Query._
import reson.util.Transformers._

import scala.collection.immutable.Iterable
import scala.util.{Failure, Try}

/**
  * Created by sscarduzio on 11/01/2016.
  */

//////// ADT
trait Query {
  def materialize: Try[String]
}

trait TableQuery extends Query {
  val table: String
}

trait Where {
  val where: Iterable[_ <: Op]
  lazy val whereString: String = if (where.isEmpty) "" else where.map(_.toString).mkString("WHERE 1 AND ", " AND ", "")
}

class TableListQ extends Query {
  def materialize: Try[String] = Try {
    s"""SELECT TABLE_SCHEMA as `schema`, TABLE_NAME as name, TABLE_TYPE = 'BASE TABLE' as insertable
        |FROM information_schema.tables
        |WHERE TABLE_SCHEMA = ${MySQL.dbConf.dbName} """
      .stripMargin
  }
}

case class UpdateQ(table: String, rows: List[DataRow], where: Ops) extends TableQuery with Where {
  def materialize: Try[String] = {
    for {
      r <- Option(rows).filter(_.nonEmpty).toTry(new Exception("No rows provided")) // SHOULD return immediately if rows is empty
      rowList <- Either.cond(r.size == 1, rows, new Exception("You should provide just one object")).toTry
      singleRow <- rowList.headOption.toTry() // Never fails
      fieldsEqValues = singleRow.map(kv => s"${kv._1}=${fixQuotes(kv._2)}")
    } yield
      s"""UPDATE $table SET ${fieldsEqValues.mkString(", ")} $whereString"""
  }
}

case class InsertQ(table: String, rows: List[DataRow]) extends TableQuery {

  def materialize: Try[String] = {
    for {
      (fields, values) <- toFieldsAndValues(rows).toTry(new Exception("Error parsing fields and values"))
    } yield s"INSERT INTO $table " + fields.mkString("(", ",", ")") + " VALUES " + values.map(_.map(fixQuotes).mkString("(", ",", ") ")).mkString(",")
  }
}

case class DeleteQ(table: String, where: Ops) extends TableQuery with Where {
  def materialize: Try[String] = {
     Option(where).filter(_.nonEmpty).map(_ => s"""DELETE FROM $table $whereString""")
      .toTry(new Exception("a where clause is needed for deletion (you need to be more explicit if you really intend to delete everything!)"))
  }
}

case class SelectQ(table: String, select: String, where: Ops, order: Option[Order], limit: Option[(Int, Int)]) extends TableQuery with Where {
  def materialize: Try[String] = Try {
    s"""SELECT $select from $table ${whereString} $formatOrder $formatLimit"""
  }

  def formatOrder: String = order.map(_.toString).getOrElse("")

  def formatLimit: String =
    limit.map(r => s"LIMIT ${r._1}, ${r._2}").getOrElse("")
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
      val wherePars = req.params.filter(s => s._1 != "select" && s._1 != "order")
      val ops = wherePars.map(parseParam)
      Option(ops.toSet).filter(_.nonEmpty).getOrElse(Set())
    }
    if (whereOps.isEmpty) "" else whereOps.map(_.toString).mkString("WHERE 1 AND ", " AND ", "")
  }

  val fixQuotes: String => String = v => if (v == null) null else s"'$v'"

}

object RequestParser {

  def parse(req: Request, table: String): Try[Query] = {

    lazy val rows: Try[List[DataRow]] = Try {
      val jo: JValue = JsonMethods.parse(req.contentString)
      val jRows = jo match {
        case JArray(arr) => arr
        case JSet(set) => set.toList
        case _ => List(jo)
      }
      jRows.map {
        case JObject(obj) => Map.from(obj map { case (k, v) =>
          k -> (v match {
            case JNothing => null
            case JNull => null
            case JString(s) => s
            case JDouble(num) => num.toString
            case JDecimal(num) => num.toString
            case JLong(num) => num.toString
            case JInt(num) => num.toString
            case JBool(v) => v.toString
            case o: JObject => compact(o)
            case a: JArray => compact(a)
            case s: JSet => compact(s)
          })
        })
        case _ =>
          throw new Exception("Cannot parse request: invalid input data")
      }
    }

    lazy val paramOps: Ops = {
      val groupedByPriority = req.params.map(parseParam).groupBy {
        case s: Select => 1
        case s: Order => 2
        case _ => 3
      }
      groupedByPriority.keySet.foldLeft(List[Op]())((acc, v) => acc ++ groupedByPriority(v))
    }

    req.method match {
      case Get => paramOps match {
        case (s: Select) :: (o: Order) :: rest => Try(SelectQ(table, s.value, rest, Option(o), rangeSuffix(req.headerMap)))
        case (s: Select) :: rest => Try(SelectQ(table, s.value, rest, None, rangeSuffix(req.headerMap)))
        case (o: Order) :: rest => Try(SelectQ(table, "*", rest, Option(o), rangeSuffix(req.headerMap)))
        case rest => Try(SelectQ(table, "*", rest, None, rangeSuffix(req.headerMap)))
      }
      case Post => rows flatMap(v => Try(InsertQ(table, v)))
      case Patch => rows flatMap(v => Try(UpdateQ(table, v, paramOps)))
      case Delete => Try(DeleteQ(table, paramOps))
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
              val fst = s"-${s.tail.takeWhile(_ != '-')}"
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

