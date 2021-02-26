package reson.util

/**
  * Created by sscarduzio on 25/01/2016.
  */
import com.twitter.{util => twitter}
import scala.concurrent.{ExecutionContext, Promise, Future}
import scala.util.{Failure, Success, Try}
import language.implicitConversions
object TwitterConverters {

  implicit def toTwitter[T](t: Try[T]): twitter.Try[T] = t match {
    case Success(r) => twitter.Return(r)
    case Failure(ex) => twitter.Throw(ex)
  }

  implicit def toScala[T](t: twitter.Try[T]): Try[T] = t match {
    case twitter.Return(r) => Success(r)
    case twitter.Throw(ex) => Failure(ex)
  }

  implicit def toTwitter[T](f: Future[T])(implicit ec: ExecutionContext): twitter.Future[T] = {
    val promise = twitter.Promise[T]()
    f.onComplete(promise update _)
    promise
  }

  implicit class TransformableTwitterFuture[T](val tFuture: twitter.Future[T]) {

    def toScala: Future[T] = {
      val promise = Promise[T]()
      tFuture.onFailure(e => promise.failure(e))
      tFuture.onSuccess(value => promise.success(value))
      promise.future
    }
  }
}