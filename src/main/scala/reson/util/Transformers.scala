package reson.util

import com.twitter.util.{Duration, Await, Promise, Future}
import scala.util.{Failure, Success, Try}


/**
  * Created by sscarduzio on 17/01/2015.
  */

object Transformers {

  implicit class TransformableTry[T](val theTry: Try[T]) {
    def toFuture: Future[T] = {
      val promise = new Promise[T]
      theTry match {
        case Success(v) => promise.setValue(v)
        case Failure(e) => promise.setException(e)
      }
      promise
    }
  }

  implicit class TransformableOption[T](val theOpt: Option[T]) {

    def toFuture(e: Exception = new Exception("option to future transformer found None")): Future[T] = {
      val promise = new Promise[T]
      theOpt match {
        case Some(s) => promise.setValue(s)
        case _ => promise.setException(e)
      }
      promise
    }

    def toTry(e: Exception = new Exception("option to try transformer found None")): Try[T] = {
      theOpt match {
        case Some(v) => Success(v)
        case _ => Failure(e)
      }
    }
  }

  implicit class TransformableEither[T](val theEither: Either[Exception, T]) {
    def toFuture: Future[T] = {
      val promise = new Promise[T]
      theEither match {
        case Right(s) => promise.setValue(s)
        case Left(e) => promise.setException(e)
      }
      promise
    }

    def toTry: Try[T] = {
      theEither match {
        case Right(s) => Success(s)
        case Left(e) => Failure(e)
      }
    }
  }

  // Don't use this to await, only completed futures will work!
  implicit class TransformableFuture[T](val theFuture: Future[T]) {
    def toTry: Try[T] = Try {
      assert(theFuture.isDefined, "should transform only materialized futures")
      Await.result(theFuture, Duration.Bottom)
    }
  }

}
