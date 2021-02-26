package reson.db

/**
  * Created by sscarduzio on 23/12/2015.
  */

import java.util.TimeZone

import com.twitter.finagle.mysql._
import org.json4s._

/**
  * Created by sscarduzio on 22/12/2015.
  */
final case class ParsingException(msg: String) extends Exception(msg)

trait MySQL2Json {

  // Assuming local and extraction time zones are UTC
  val tz: TimeZone = TimeZone.getTimeZone("UTC")

  def mkValue[V <: Value](key: String, v: V): JField = v match {
    case NullValue => key -> JNull
    case StringValue(s) => key -> JString(s)
    case IntValue(i) => key -> JInt(i)
    case LongValue(l) => key -> JLong(l)
    // MySQL uses TinyInts to represent booleans
    case ByteValue(b) => key -> JBool(b != 0)
    case ShortValue(s) => key -> JInt(s)
    case FloatValue(f) => key -> JDouble(f)
    case DoubleValue(d) => key -> JDouble(d)
    case BigDecimalValue(bd) => key -> JDecimal(bd)
    case RawValue(typ, charset, isBinary, bytes) =>
      val parsed: String = typ match {
        case 12 | 7 =>
          new TimestampValue(tz, tz).unapply(v).map(_.getTime.toString)
            .getOrElse(throw ParsingException(s"cannot parse ~timestamp $v"))
        case 10 =>
          DateValue.unapply(RawValue(Type.Date, charset, isBinary, bytes))
            .map(_.getTime.toString)
            .getOrElse(throw ParsingException(s"cannot parse ~date $v"))
        case _ => throw ParsingException(
          s"""Unsupported column type: $typ for key: $key""")
      }
      key -> JString(parsed)
    case EmptyValue => key -> JString("")
    case _ => throw ParsingException(s"""Unsupported column $key => $v""")

  }

  implicit val mysqlRowToJsonSerializer: Row => JObject = { row =>
    val zippedMap = row.fields.zip(row.values)
    JObject(zippedMap.map(t => mkValue(t._1.name, t._2)):_*)
  }
}
