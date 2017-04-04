package fommil

import scala.annotation.StaticAnnotation
import scala.collection.immutable.Seq
import scala.meta._

class data extends StaticAnnotation {

  inline def apply(defn: Any): Any = meta {
    defn match {
      case cls @ Defn.Class(_, name, _, ctor, _) =>
        val params = ctor.paramss match {
          case params :: Nil => params
          case _             => abort("Can't create generic for curried functions yet.")
        }
        val clsN =
          q"""final class ${Type.Name(name.value)} private (...${ctor.paramss})
              extends Serializable with Product {
                // Members declared in scala.Equals
                def canEqual(that: Any): Boolean = ???

                // Members declared in scala.Product
                def productArity: Int = ???
                def productElement(n: Int): Any = ???
              }
          """
        val args = params.map { param => Term.Name(param.name.value) }
        val companion =
          q"""object ${Term.Name(name.value)} extends Serializable {
                override def toString = ${name.value}
                def apply(...${ctor.paramss}) = new ${Ctor.Ref.Name(name.value)}(..$args)
              }
           """
        Term.Block(Seq(clsN, companion))
      case defn: Tree =>
        println(defn.structure)
        abort("@data must annotate a class or a sealed trait/class.")
    }
  }
}
