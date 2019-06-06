package play.modules.reactivemongo

import org.reactivestreams.Publisher

import scala.concurrent.Future

import akka.stream.scaladsl.Sink

import play.api.libs.iteratee.{ Enumerator, Iteratee }

import play.api.libs.iteratee.streams.{ IterateeStreams => Underlying }

private[reactivemongo] object Streams {
  def enumeratorToPublisher[T](enum: Enumerator[T], emptyElem: Option[T] = None): Publisher[T] = Underlying.enumeratorToPublisher[T](enum, emptyElem)

  def iterateeToSink[T, U](iter: Iteratee[T, U]): Sink[T, Future[U]] = {
    val (sub, it) = Underlying.iterateeToSubscriber[T, U](iter)
    Sink.fromSubscriber(sub).mapMaterializedValue { _ => it.run }
  }
}
