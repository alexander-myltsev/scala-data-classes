package testing.meta

import fommil.stalagmite.data

import _root_.scala._
import _root_.scala.Predef._
import cats.{ Functor, Monad }
import shapeless.cachedImplicit

import scala.annotation.tailrec

@data(
  product = true,
  checkSerializable = false
)
class FooMeta[+T](a: Boolean, s: String, t: T, i: Int = 0)

@data(
  memoise = true,
  memoiseRefs = Seq('s),
  memoiseHashCode = true,
  memoiseToString = true,
  memoiseStrong = true
)
class FooMetaMemoised(a: Boolean, s: String)

@data(
  memoise = true,
  memoiseRefs = Seq('s)
)
class FooMetaMemoisedWeak(a: Boolean, s: String)

@data(
  optimiseHeapOptions = true,
  optimiseHeapBooleans = true,
  optimiseHeapStrings = true,
  product = true
)
class FooMetaOptimiseHeap(a: Option[Boolean],
                          b: Option[Boolean],
                          s: Option[String])

object Implicits {
  implicit def FooDerivingFunctorInstance: Functor[FooMetaDeriveTypes] =
    new Functor[FooMetaDeriveTypes] {
      def map[A, B](
        fa: FooMetaDeriveTypes[A]
      )(f: (A) => B): FooMetaDeriveTypes[B] =
        fa.copy(t = f(fa.t))
    }

  implicit def FooDerivingMonadInstance: Monad[FooMetaDeriveTypes] =
    new Monad[FooMetaDeriveTypes] {
      override def pure[A](x: A): FooMetaDeriveTypes[A] =
        FooMetaDeriveTypes(x, true)

      override def flatMap[A, B](fa: FooMetaDeriveTypes[A])(
        f: (A) => FooMetaDeriveTypes[B]
      ): FooMetaDeriveTypes[B] = FooMetaDeriveTypes(f(fa.t).t, fa.b)

      @tailrec
      override def tailRecM[A, B](
        a: A
      )(f: (A) => FooMetaDeriveTypes[Either[A, B]]) =
        f(a) match {
          case FooMetaDeriveTypes(Right(x), b) => FooMetaDeriveTypes[B](x, b)
          case FooMetaDeriveTypes(Left(x), b)  => tailRecM(x)(f)
        }
    }
}

import Implicits._

@data(
  deriving = Seq('Functor, 'Monad),
  serializable = false
)
class FooMetaDeriveTypes[T](t: T, b: Boolean)
