package reson.util

import com.twitter.util.{Promise, Future}
import scala.util.{Failure, Success, Try}


/**
  * Created by sscarduzio on 17/01/2015.
  */

object Transformers {

  implicit class TransformableTry[T](val theTry: Try[T]) {
    def future[T] = tryToFuture(theTry)

    def tryToFuture[T](theTry: Try[T]): Future[T] = {
      val promise = new Promise[T]
      theTry match {
        case Success(v) => promise.setValue(v)
        case Failure(e) => promise.setException(e)
      }
      promise
    }
  }

  implicit class TransformableOption[T](val theOpt: Option[T]) {

    def future[T](theOpt: Option[T], e: Exception = new Exception("option to future transformer found None")): Future[T] = {
      val promise = new Promise[T]
      theOpt match {
        case Some(s) => promise.setValue(s)
        case None => promise.setException(e)
      }
      promise
    }
  }

}
