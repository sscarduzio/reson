package reson

import scala.annotation.tailrec

/**
  GET /people?select=age,height,weight
  GET /people?age=gte.18&student=is.true
  GET /people?age=lt.13
  GET /people?age=not.eq.2
  GET /people?order=age.desc,height.asc
  GET /people?order=age
  GET /people?order=age.nullsfirst
  GET /people?order=age.desc.nullslast

  * if path is null, don't parse params.

  * key is always select, order or a column
  * select and order are handled separately
  * values for column keys are always operators
  * operators can be handled separately.
  */


sealed trait Op {
  override def toString = "__unimplemented__"
}

case object NoOp extends Op {
  override def toString = ""
}

trait Negatable {
  val not: Boolean
  def isNot: String = if (not) " NOT " else " "
}

final case class SingleOp(column: String, value: String, key: String) extends Op {
  override def toString = s"""$column $key '$value'"""
}

class SingleNegatableOp(column: String, val not: Boolean, value: String, key: String) extends Op with Negatable {
  override def toString = s"""$column $isNot $key '$value'"""
}

class MultiOp(val key: String, values: Seq[String]) extends Op {
  override def toString = s""" $key ${values.map(x => s"'$x'").mkString(",")}"""
}

final case class Like(column:String, override val not:Boolean, value:String) extends SingleNegatableOp(column, not, value, "LIKE") {
 override def toString: String = s"""${super.toString}""".replace("*","%") // #TODO enhancement (not in PostgREST) single wildcard (question mark) support
}

final case class ILike(column:String, override val not:Boolean, value:String) extends SingleNegatableOp(column, not, value, "COLLATE UTF8_GENERAL_CI LIKE") {
  override def toString: String = s"""${super.toString}""".replace("*","%") // #TODO enhancement (not in PostgREST) single wildcard (question mark) support
}

final case class Order(values: Seq[String]) extends MultiOp("order", values) {

  // order=age.desc,height.asc
  // order=age.nullsfirst
  // order=age.desc.nullslast
  case class Rule(col: String, desc: Boolean = false, asc: Boolean = false, nullsFirst: Boolean = false, nullsLast: Boolean = false) {
    if (desc && asc || nullsFirst && nullsLast) throw new Exception("invalid ordering rule")

    override def toString: String =
      s"""$col ${if (desc) "DESC" else ""}${if (asc) "ASC" else ""} """ +
        s"""${if (nullsFirst) "NULLS FIRST" else ""}${if (nullsLast) "NULLS LAST" else ""}"""
  }

  private val rules = values.map { rule =>
    val spl = rule.split("\\.")
    spl.toList match {
      case col :: Nil => Rule(col)
      case col :: v => Rule(col, v.contains("desc"), v.contains("asc"), v.contains("nullsfirst"), v.contains("nullslast"))
      case _ => throw new Exception("unparsable ordering rule: " + rule)
    }
  }

  override def toString: String = "ORDER BY " + rules.mkString(", ")
}

final case class InOp(column:String, values:String, not: Boolean) extends Op with Negatable {
  override def toString: String = s"""`$column`${isNot}IN """ + values.stripPrefix("(").stripSuffix(")").split(',').map(v => s"'$v'").mkString("(", ",", ")")
}

final case class Select(value:String) extends Op {
  override def toString: String = "SELECT " + value
}

// END OF ADT

object ParameterParser {

  def parseParam(kv: (String, String)): Op = parseParam(kv._1, kv._2)

  def parseParam(key: String, value: String): Op = {
    def noNeg(n: Boolean): Unit = if (n) throw new Exception("meaningless not")

    @tailrec
    def parse(key: String, values: Seq[String], not: Boolean): Op = {
      (key, values) match {
        case ("select", _) => noNeg(not); Select(value)
        case ("order", _) => noNeg(not); Order(value.split(',').toIndexedSeq)
        case (_, "not" :: v) => parse(key, v, not = true)
        case (col, "in" :: v) => InOp(col, v.head, not)
        case (col, "eq" :: v) => noNeg(not); SingleOp(col, v.head, "=")
        case (col, "gt" :: v) => noNeg(not); SingleOp(col, v.head, ">")
        case (col, "gte" :: v) => noNeg(not); SingleOp(col, v.head, ">=")
        case (col, "lt" :: v) => noNeg(not); SingleOp(col, v.head, "<")
        case (col, "lte" :: v) => SingleOp(col, v.head, "<=")
        case (col, "like" :: v) => Like(col, not, v.head)
        case (col, "ilike" :: v) => ILike(col, not, v.head)
        case (_, _) =>
          throw new Exception(s"nothing matched key=$key, value=$value")
      }
    }
    parse(key, value.split("\\.").toList, not = false)
  }
}