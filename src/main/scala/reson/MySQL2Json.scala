package reson

/**
  * Created by sscarduzio on 23/12/2015.
  */
package tonno.api
import java.util.TimeZone

import com.twitter.finagle.exp.mysql._
import rapture.json._
import rapture.json.jsonBackends.argonaut._


/**
  * Created by sscarduzio on 22/12/2015.
  */
trait MySQL2Json {

  // Assuming local and extraction time zones are UTC
  val tz = TimeZone.getTimeZone("UTC")

  def mkValue[V <: Value](key: String, v: V): String = v match {
    case StringValue(s) => s"""{"$key" : "$s" }"""
    case IntValue(i) => s"""{"$key" : $i }"""
    case LongValue(l) => s"""{"$key" : $l }"""
    // MySQL uses TinyInts to represent booleans
    case ByteValue(b) => s"""{"$key" : ${b != 0} }"""
    case RawValue(typ, charset, isBynary, bytes) => {
      val ts:Long = new TimestampValue(tz, tz).unapply(v).get.getTime
      s"""{"$key" : $ts }"""
    }
    case ShortValue(s) => s"""{"$key" : $s }"""
    case FloatValue(f) => s"""{"$key" : $f }"""
    case BigDecimalValue(bd) => s"""{"$key" : ${bd.floatValue} }"""
    case _ => s"""{"$key" : null }"""
  }

  implicit val mysqlRowToJsonSerializer = Json.serializer[Json].contramap[Row] { row =>
    val zippedMap = row.fields.zip(row.values)
    val listOfJson: IndexedSeq[String] = zippedMap.map(t => mkValue(t._1.name, t._2))

    val jb = listOfJson.map(j => JsonBuffer.parse(j)).foldLeft(JsonBuffer.empty)(_ ++ _)
    Json(jb)
  }
}
