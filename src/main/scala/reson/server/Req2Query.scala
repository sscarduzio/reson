package reson.server

import com.twitter.finagle.http.{HeaderMap, Request}
import reson.Param2Op._
import reson.{Op, NoOp, RequestNotSatisfiable}

import scala.util.Try

/**
  * Created by sscarduzio on 01/01/2016.
  */
object Req2Query {

  def parse(req: Request, table: String): String = {
    val params = req.params.toMap
    val select = parseParam("select", params.get("select").getOrElse("*"))
    val order = params.get("order").map(parseParam("order", _)).getOrElse(NoOp)
    val restOfOps: Set[Op] = {
      val pars = params.filter(v => v._1 != "select" && v._1 != "order")
      val ops = pars.map(parseParam)
      Option(ops.toSet).filter(!_.isEmpty).getOrElse(Set())
    }
    val qs: Set[String] =
      Set(
        select.toString + s" from $table",
        restOfOps.map(_.toString).mkString("WHERE 1 AND ", "AND ", ""),
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
    range.map(r => s"LIMIT ${r._1} ,${r._2}").getOrElse("")
  }
}
