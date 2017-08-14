package fommil

import testing.meta._

import _root_.scala._
import _root_.scala.Predef._

import org.scalatest._
import org.scalatest.Matchers._
import stalagmite.data
import spray.json._
import shapeless.cachedImplicit

class DerivationsSpec extends FlatSpec with ParallelTestExecution {

  val fooMeta = FooMetaDerive(true, "hello", 2)

  "@data(generic) class Foo" should "convert to Json" in {
    fooMeta.toJson shouldBe JsObject("a" -> JsBoolean(fooMeta.a),
                                     "s" -> JsString(fooMeta.s),
                                     "i" -> JsNumber(fooMeta.i))
  }

  it should "allow user-land Semigroup (Generic) derivation" in {
    pending
//    (fooMeta |+| fooMeta) should equal(
//      Types.FooMetaDeriveTypes(true, "hellohello", 4)
//    )
  }

  it should "allow user-land JsonFormat (LabelledGeneric) derivation" in {
    fooMeta.toJson.compactPrint should equal(
      """{"a":true,"s":"hello","i":2}"""
    )
  }

  it should "not be JsonFormat derivation by default" in {
    """@data(generic = Seq())
      |class FooNotJson(i: Int)
      |FooNotJson(42).toJson""".stripMargin shouldNot compile
  }
}
