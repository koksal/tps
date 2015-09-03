package tps.synthesis

import tps.util.UniqueCounter

object Trees {
  import TypeTrees._

  val uniqueCounter = new UniqueCounter()

  object FreshIdentifier {
    def apply(name: String): Identifier = {
      new Identifier(name, uniqueCounter.next)
    }
  }

  class Identifier(val name: String, val id: Int) extends Typed {
    override def equals(other: Any): Boolean = {
      if (other == null || !other.isInstanceOf[Identifier])
        false
      else
        this.id == other.asInstanceOf[Identifier].id
    }

    override def toString: String = name

    def uniqueName: String = name + id

    def toVariable: Variable = Variable(this)
  }

  def mkFreshIntVar(name: String): Variable = {
    FreshIdentifier(name).setType(IntType).toVariable
  }

  def mkFreshBooleanVar(name: String): Variable = {
    FreshIdentifier(name).setType(BooleanType).toVariable
  }

  sealed abstract class Expr extends Typed

  class And(val exprs: Seq[Expr]) extends Expr with FixedType {
    val fixedType = BooleanType
  }

  object And {

    def apply(exprs: Expr*): Expr = {
      if (exprs.isEmpty) BooleanLiteral(true) else new And(exprs)
    }

    def unapply(and: And): Option[Seq[Expr]] = {
      if (and == null) None else Some(and.exprs)
    }

  }

  class Or(val exprs: Seq[Expr]) extends Expr with FixedType {
    val fixedType = BooleanType
  }

  object Or {

    def apply(exprs: Expr*): Expr = {
      if (exprs.isEmpty) BooleanLiteral(false) else new Or(exprs)
    }
  
    def unapply(or: Or): Option[Seq[Expr]] = {
      if (or == null) None else Some(or.exprs)
    }

  }

  case class Iff(val left: Expr, val right: Expr) extends Expr with FixedType {
    val fixedType = BooleanType
  }

  case class Implies(val left: Expr, val right: Expr) 
      extends Expr with FixedType {
    val fixedType = BooleanType    
  }

  case class Not(val expr: Expr) extends Expr with FixedType {
    val fixedType = BooleanType
  }

  case class Equals(val left: Expr, val right: Expr) 
      extends Expr with FixedType {
    val fixedType = BooleanType
  }

  case class Variable(id: Identifier) extends Expr {
    override def getType: TypeTree = id.getType
    override def setType(tt: TypeTree) = {
      id.setType(tt)
      this
    }
  }

  case class IntLiteral(value: Int) extends Expr with FixedType {
    val fixedType = IntType
  }

  case class BooleanLiteral(value: Boolean) extends Expr with FixedType {
    val fixedType = BooleanType
  }

  case class Plus(left: Expr, right: Expr) extends Expr with FixedType {
    val fixedType = IntType
  }

  case class LessThan(left: Expr, right: Expr) extends Expr with FixedType {
    val fixedType = BooleanType
  }

  case class GreaterThan(left: Expr, right: Expr) extends Expr with FixedType {
    val fixedType = BooleanType
  }

  case class LessEquals(left: Expr, right: Expr) extends Expr with FixedType {
    val fixedType = BooleanType
  }

  case class GreaterEquals(left: Expr, right: Expr) extends Expr with FixedType {
    val fixedType = BooleanType
  }
}
