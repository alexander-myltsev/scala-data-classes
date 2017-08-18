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

  val fooMetaGeneric    = FooMetaGeneric(true, "hello", 2)
  val fooMetaSeqGeneric = FooMetaGenericSeq(true, "hello", 2)

  "@data(generic) class Foo" should "convert to Json" in {
    fooMetaSeqGeneric.toJson shouldBe JsObject(
      "a" -> JsBoolean(fooMetaSeqGeneric.a),
      "s" -> JsString(fooMetaSeqGeneric.s),
      "i" -> JsNumber(fooMetaSeqGeneric.i)
    )
  }

  it should "allow user-land Semigroup (Generic) derivation" in {
    pending
//    (fooMeta |+| fooMeta) should equal(
//      Types.FooMetaDeriveTypes(true, "hellohello", 4)
//    )
  }

  it should "allow user-land JsonFormat (LabelledGeneric) derivation" in {
    fooMetaGeneric.toJson.compactPrint should equal(
      """{"a":true,"s":"hello","i":2}"""
    )
    fooMetaSeqGeneric.toJson.compactPrint should equal(
      """{"a":true,"s":"hello","i":2}"""
    )
  }

  it should "not be JsonFormat derivation by default" in {
    """@data()
      |class FooNotJson(i: Int)
      |implicitly[JsFormat[FooNotJson]].toJson""".stripMargin shouldNot compile
  }
}
