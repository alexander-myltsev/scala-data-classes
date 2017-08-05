package fommil

import testing.meta._

import _root_.scala._
import _root_.scala.Predef._

import org.scalatest._
import org.scalatest.Matchers._
import stalagmite.data

import cats._
import cats.data._
import cats.implicits._

class DerivedDerivationsSpec extends FlatSpec with ParallelTestExecution {

  val fooMeta = FooMetaDeriveTypes[Int](42, false)

  "@data(deriving) class Foo[+]" should "be Functor instance" in {
    val mapFun        = (x: Int) => x.toString
    val fooMetaMapped = fooMeta.map(mapFun)
    fooMetaMapped should equal(fooMeta.copy(t = mapFun(fooMeta.t)))
  }

  it should "be Monad instance" in {
    val mapFun   = (x: Int, y: Int) => (x + y).toString
    val fooMeta1 = FooMetaDeriveTypes(24, true)
    val result   = for { fm <- fooMeta; fm1 <- fooMeta1 } yield mapFun(fm, fm1)
    result should equal(fooMeta.copy(t = mapFun(fooMeta.t, fooMeta1.t)))
  }

  it should "be neither Monad nor Functor instance by default" in {
    """implicit def AFunctorInstance: Functor[NoFunctor] =
      |  new Functor[NoFunctor] {
      |    override def map[A, B](fa: NoFunctor[A])(f: (A) => B) =
      |      fa.copy(t = f(fa.t))
      |  }
      |
      |@data(deriving = Seq(), serializable = false)
      |class NoFunctor[T](t: T)
      |NoFunctor[Int](42).map(x => x)""".stripMargin should compile
  }
}
