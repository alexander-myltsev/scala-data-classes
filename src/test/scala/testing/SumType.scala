package testing

import fommil.stalagmite.{ data, Deriving }
import _root_.scala._
import _root_.scala.Predef._

@data(deriving = Seq(Deriving.Functor, Deriving.Monad))
//@data(deriving = Seq(Deriving.Monad))
class SumType(b: Boolean)
