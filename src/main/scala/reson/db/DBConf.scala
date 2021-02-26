package reson.db

/**
  * Created by sscarduzio on 25/12/2015.
  */

import scala.util.Try

case class HostAndPort(host: String, port: Int = -1) {
  def hasPort: Boolean = port >= 0

  override def toString: String = {
    val h = if (host contains ":") s"[$host]" else host
    if (hasPort) s"$h:$port" else h
  }
}

object HostAndPort {

  def fromString(s: String): HostAndPort = (s split ':').toList.reverse match {
    case p :: h if p.toIntOption.isDefined =>
      HostAndPort(h.reverse mkString ":", p.toInt)
    case _ => HostAndPort(s)
  }

  def fromParts(h: String, p: Int): HostAndPort = HostAndPort(h, p)

}

final case class ConfigurationError(msg: String) extends Exception

final case class ConnectionConfig(proto: String, user: String, pass: Option[String], private val _hostAndPort: HostAndPort, dbName: String) {
  val defaultPort = if (proto.toLowerCase == "mysql") 3306 else 5432
  val hostAndPort = if (!_hostAndPort.hasPort) HostAndPort.fromParts(_hostAndPort.host, defaultPort) else _hostAndPort

  override def toString = s"""DBConf parsed: $proto://$user:${pass.getOrElse("")}@$hostAndPort/$dbName"""

  println(this)
}

object ConnectionConfig {
  def apply(dbUri: String): ConnectionConfig = {
    dbUri.split( """://|@""") match {
      case Array(proto, credentials, hpdb) => {
        val userAndPass = credentials.split(":")
        val hostAndPortAndDB = hpdb.split("/")
        ConnectionConfig(proto, userAndPass(0), Try(userAndPass(1)).toOption, HostAndPort.fromString(hostAndPortAndDB(0)), hostAndPortAndDB(1))
      }
      case _ => throw new ConfigurationError("connection URL (env. var: db_uri) should be in the form: mysql://user:pass@localhost:3306/db_name ")
    }
  }
}