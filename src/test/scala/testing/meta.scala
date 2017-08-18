package testing.meta

import fommil.stalagmite.data

import _root_.scala._
import _root_.scala.Predef._
import shapeless.cachedImplicit

import scala.annotation.tailrec
import spray.json._
import fommil.sjs.FamilyFormats._

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

@data(generic = JsonFormat)
class FooMetaGeneric(a: Boolean, s: String, i: Int = 0)

@data(generic = Seq(JsonFormat))
class FooMetaGenericSeq(a: Boolean, s: String, i: Int = 0)
