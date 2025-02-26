package io.scalaland.chimney.internal.macros

import scala.reflect.macros.blackbox

trait Model extends TransformerConfigSupport {

  val c: blackbox.Context

  import c.universe.*

  case class Target(name: String, tpe: Type)
  object Target {
    def fromJavaBeanSetter(ms: MethodSymbol, site: Type): Target =
      Target(ms.canonicalName, ms.beanSetterParamTypeIn(site))

    def fromField(ms: MethodSymbol, site: Type): Target =
      Target(ms.canonicalName, ms.resultTypeIn(site))
  }

  case class DerivedTree(tree: Tree, target: DerivationTarget) {
    def isTotalTarget: Boolean = target == DerivationTarget.TotalTransformer
    def isPartialTarget: Boolean = target.isInstanceOf[DerivationTarget.PartialTransformer]
    def isLiftedTarget: Boolean = target.isInstanceOf[DerivationTarget.LiftedTransformer]

    def mapTree(f: Tree => Tree): DerivedTree = copy(tree = f(tree))
    def callTransform(input: Tree): DerivedTree = mapTree { tree =>
      target match {
        case DerivationTarget.TotalTransformer =>
          tree.callTransform(input)
        case DerivationTarget.PartialTransformer(failFastTermName) =>
          tree.callPartialTransform(input, q"$failFastTermName")
        case _: DerivationTarget.LiftedTransformer =>
          tree.callTransform(input)
      }
    }
  }
  object DerivedTree {
    def fromTotalTree(tree: Tree): DerivedTree = DerivedTree(tree, DerivationTarget.TotalTransformer)
  }

  case class InstanceClause(matchName: Option[TermName], matchTpe: Type, body: DerivedTree) {
    def toPatMatClauseTree: Tree = {
      matchName match {
        case Some(name) =>
          // in general pat var name is not tracked whether it was used in body tree
          // introducing synthetic val _ helps avoid reporting unused warnings in macro-generated code
          cq"$name: $matchTpe => { val _ = $name; ${body.tree} }"
        case None => cq"_: $matchTpe => ${body.tree}"
      }
    }
    def mapBody(f: DerivedTree => DerivedTree): InstanceClause = copy(body = f(body))
  }

  sealed trait AccessorResolution extends Product with Serializable {
    def isResolved: Boolean
  }
  object AccessorResolution {
    case object NotFound extends AccessorResolution {
      override def isResolved: Boolean = false
    }
    case class Resolved(symbol: MethodSymbol, wasRenamed: Boolean) extends AccessorResolution {
      override def isResolved: Boolean = true
    }
    case object DefAvailable extends AccessorResolution {
      override def isResolved: Boolean = false
    }
  }
}
