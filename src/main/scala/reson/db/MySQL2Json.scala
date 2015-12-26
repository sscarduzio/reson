package reson.db

/**
  * Created by sscarduzio on 23/12/2015.
  */
import java.util.{Date, TimeZone}

import com.twitter.finagle.exp.mysql._
import rapture.json._
import rapture.json.jsonBackends.argonaut._


/**
  * Created by sscarduzio on 22/12/2015.
  */
case class ParsingException(msg:String)extends Exception

trait MySQL2Json {

  // Assuming local and extraction time zones are UTC
  val tz = TimeZone.getTimeZone("UTC")

  def mkValue[V <: Value](key: String, v: V): String = v match {

    case StringValue(s) => s"""{"$key" : "$s" }"""
    case IntValue(i) => s"""{"$key" : $i }"""
    case LongValue(l) => s"""{"$key" : $l }"""
    // MySQL uses TinyInts to represent booleans
    case ByteValue(b) => s"""{"$key" : ${b != 0} }"""
    case ShortValue(s) => s"""{"$key" : $s }"""
    case FloatValue(f) => s"""{"$key" : $f }"""
    case DoubleValue(d) => s"""{"$key" : $d}"""
    case BigDecimalValue(bd) => s"""{"$key" : ${bd.floatValue} }"""
    case RawValue(typ, charset, isBinary, bytes) => {
      val parsed:String = typ match {
        case 12|7 => new TimestampValue(tz, tz).unapply(v).map(_.getTime.toString)
          .getOrElse(throw new ParsingException(s"cannot parse ~timestamp $v"))
        case 10 =>  DateValue.unapply(RawValue(Type.Date,charset, isBinary,bytes)).map(_.getTime.toString)
          .getOrElse(throw new ParsingException(s"cannot parse ~date $v"))
        case _ =>  throw new ParsingException(s"""Unsupported column type: $typ for key: $key""")
      }
      s"""{"$key" : "$parsed" }"""
    }
    case _ => throw new ParsingException(s"""Unsupported column $key => $v""")

  }

  implicit val mysqlRowToJsonSerializer = Json.serializer[Json].contramap[Row] { row =>
    val zippedMap = row.fields.zip(row.values)
    val listOfJson: IndexedSeq[String] = zippedMap.map(t => mkValue(t._1.name, t._2))

    val jb = listOfJson.map(j => JsonBuffer.parse(j)).foldLeft(JsonBuffer.empty)(_ ++ _)
    Json(jb)
  }
}
