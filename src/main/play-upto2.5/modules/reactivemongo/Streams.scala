package play.modules.reactivemongo

import org.reactivestreams.Publisher

import scala.concurrent.Future

import akka.stream.scaladsl.Sink

import play.api.libs.iteratee.{ Enumerator, Iteratee }

import play.api.libs.streams.{ Streams => Underlying }

private[reactivemongo] object Streams {
  def enumeratorToPublisher[T](enum: Enumerator[T], emptyElem: Option[T] = None): Publisher[T] = Underlying.enumeratorToPublisher[T](enum, emptyElem)

  def iterateeToSink[T, U](iter: Iteratee[T, U]): Sink[T, Future[U]] =
    Underlying.iterateeToAccumulator[T, U](iter).toSink
}

