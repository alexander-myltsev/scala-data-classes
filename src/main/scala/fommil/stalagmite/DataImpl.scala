// Copyright: 2017 https://github.com/fommil/stalagmite/graphs
// License: http://www.apache.org/licenses/LICENSE-2.0
package fommil.stalagmite

import scala.collection.immutable.Seq
import scala.meta._

object DataImpl {

  import stats.CaseClassStats._
  import stats.MemoisationStats._
  import stats.SerializableStats._
  import stats.ShapelessStats._
  import stats.HeapOptimizationStats._

  def validateClass(ctor: Ctor.Primary,
                    tmpl: Template,
                    tparams: Seq[Type.Param]) = {
    if (ctor.paramss.length > 1) {
      abort("Current implementation doesn't support curried class definitions")
    }

    if (ctor.paramss.flatten.exists(
          param => param.mods.exists(_ != Mod.ValParam())
        )) {
      abort("Fields should be only vals, without modifiers")
    }

    if (ctor.paramss.flatten.isEmpty) {
      abort("Class shouldn't be empty")
    }

    if (ctor.mods.nonEmpty) {
      abort("Current implementation doesn't support constructor params")
    }

    if (tmpl.early.nonEmpty) {
      abort("Current implementation doesn't support early initialization")
    }

    if (tmpl.parents.nonEmpty) {
      abort("Current implementation doesn't support inheritance")
    }

    tmpl.self.name match {
      case _: Name.Anonymous =>
      case _                 => abort("Current implementation doesn't support self type")
    }

    if (tmpl.stats.map(_.length).getOrElse(0) != 0) {
      abort("Current implementation doesn't support statements in class body")
    }
  }

  def validateDataInfo(dataInfo: DataInfo) =
    for {
      ref <- dataInfo.dataMods.memoiseRefs
    } {
      if (!dataInfo.classParamNames.map(_.value).contains(ref))
        abort(s"""There's no field called $ref""")
    }

  def buildClass(dataInfo: DataInfo, builders: Seq[DataStats]): Stat = {
    val actualFields = if (dataInfo.requiredToPack) {
      dataInfo.optimizedParams
    } else {
      dataInfo.classParamsWithTypes
    }
    val ctorParams = actualFields.map {
      case (param, tpe) => param"""private[this] var
               ${Term.Name("_" + param.value)}: $tpe"""
    }

    val modsToClasses: Seq[(DataInfo => Boolean, Ctor.Call)] = Seq(
      ((di: DataInfo) => di.dataMods.product) ->
        ctor"_root_.scala.Product",
      ((di: DataInfo) => di.dataMods.serializable) ->
        ctor"_root_.scala.Serializable"
    )
    val extendsClasses = modsToClasses.collect {
      case (dataInfoP, ctor) if dataInfoP(dataInfo) => ctor
    }

    q"""final class ${dataInfo.name}
          [..${dataInfo.simpleTypeParams}] private (..$ctorParams)
          extends ..${Seq(extendsClasses: _*)} {

       import _root_.scala._
       import _root_.scala.Predef._

       ..${builders.flatMap(_.classStats(dataInfo))}
    }"""
  }

  def buildObject(dataInfo: DataInfo, builders: Seq[DataStats]): Stat = {
    val modsToClasses: Seq[(DataInfo => Boolean, Ctor.Call)] = Seq(
      ((di: DataInfo) => di.dataMods.serializable) ->
        ctor"_root_.scala.Serializable"
    ) ++ (if (dataInfo.typeParams.isEmpty) {
            Seq(
              ((di: DataInfo) => di.dataMods.companionExtends) ->
                ctor"((..${dataInfo.classParamsTypes}) => ${dataInfo.dataType})"
            )
          } else {
            Seq()
          })

    val extendsClasses = modsToClasses.collect {
      case (dataInfoP, ctor) if dataInfoP(dataInfo) => ctor
    }

    q"""object ${Term.Name(dataInfo.name.value)}
          extends ..${Seq(extendsClasses: _*)} {
       import _root_.scala._
       import _root_.scala.Predef._

       ..${builders.flatMap(_.objectStats(dataInfo))}
    }"""
  }

  def expand(clazz: Defn.Class, dataMods: DataMods): Term.Block =
    clazz match {
      case cls @ Defn.Class(Seq(), name, tparams, ctor, tmpl) =>
        validateClass(ctor, tmpl, tparams)
        val dataInfo = DataInfo(
          name,
          ctor.paramss.flatten,
          tparams,
          dataMods
        )
        validateDataInfo(dataInfo)

        val modsToStats: Seq[(DataInfo => Boolean, Seq[DataStats])] = Seq(
          ((di: DataInfo) => di.dataMods.product) -> Seq(
            DataProductMethodsStats
          ),
          ((di: DataInfo) => di.dataMods.serializable) -> Seq(
            DataWriteObjectStats,
            DataReadObjectStats,
            DataReadResolveStats
          ),
          ((di: DataInfo) => di.dataMods.shapeless) -> Seq(
            DataShapelessBaseStats,
            DataShapelessTypeableStats,
            DataShapelessGenericsStats
          ),
          ((di: DataInfo) => di.dataMods.memoise) -> Seq(
            DataMemoiseStats
          ),
          ((di: DataInfo) => di.requiredToPack) -> Seq(
            PackStats
          ),
          ((di: DataInfo) => di.dataMods.deriving.nonEmpty) -> Seq(
            DataDerivationStats
          )
        )

        val builders = Seq(
          DataApplyStats,
          DataUnapplyStats,
          if (dataInfo.requiredToPack) UnpackGettersStats else DataGettersStats,
          DataEqualsStats,
          DataHashCodeStats,
          DataToStringStats,
          DataCopyStats
        ) ++ modsToStats.collect {
          case (dataInfoP, bs) if dataInfoP(dataInfo) => bs
        }.flatten.toList

        val newClass  = buildClass(dataInfo, builders)
        val newObject = buildObject(dataInfo, builders)
        println((newClass, newObject).toString())

        Term.Block(
          Seq(
            newClass,
            newObject
          )
        )
      case _ =>
        abort("@data must annotate a class without mods.")
    }
}

object Deriving extends Enumeration {
  type Deriving = Value
  val Monad, Functor = Value
}

class data(deriving: scala.Seq[Deriving.Value] =
             scala.collection.immutable.Seq.empty[Deriving.Value],
           product: Boolean = false,
           checkSerializable: Boolean = true,
           companionExtends: Boolean = false,
           serializable: Boolean = true,
           shapeless: Boolean = true,
           memoise: Boolean = false,
           memoiseHashCode: Boolean = false,
           memoiseToString: Boolean = false,
           memoiseStrong: Boolean = false,
           memoiseRefs: scala.Seq[scala.Symbol] = scala.Seq(),
           optimiseHeapOptions: Boolean = false,
           optimiseHeapBooleans: Boolean = false,
           optimiseHeapStrings: Boolean = false)
    extends scala.annotation.StaticAnnotation {

  // workaround for https://github.com/scalameta/scalameta/issues/1038
  val dummy = 'dummy

  inline def apply(defn: Any): Any = meta {

    val dataMods = this match {
      case q"new data(..$args)" =>
        val pairs = args.collect {
          case Term.Arg.Named(Term.Name(name), Lit.Boolean(b)) =>
            name -> Left(b)
          case Term.Arg.Named(
              Term.Name("memoiseRefs"),
              Term.Apply(Term.Name("Seq"), symbols)
              ) =>
            "memoiseRefs" -> Right(symbols.collect {
              case q"scala.Symbol(${Lit.String(sym) })" => sym
            })
          case Term.Arg.Named(
              Term.Name("deriving"),
              Term.Apply(Term.Name("Seq"), symbols)
              ) =>
            "deriving" -> Right(symbols.collect {
              case q"Deriving.${Term.Name(sym) }" => sym
            })
        }
        DataMods.fromPairs(pairs)

      case _ => DataMods()
    }

    defn match {
      case Term.Block(
          Seq(cls @ Defn.Class(Seq(), name, Seq(), ctor, tmpl),
              companion: Defn.Object)
          ) =>
        abort("@data block")
      case clazz: Defn.Class => DataImpl.expand(clazz, dataMods)
    }
  }
}
