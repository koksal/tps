package tps.synthesis

import z3.scala._

import language.existentials

import Trees._
import TypeTrees._

import tps.UndirectedGraphs._
import tps.GraphSolutions._

import tps.util.LogUtils

abstract class AbstractSymbolicSolver(
  val opts: SynthesisOptions,
  val interpretation: Interpretation
) extends Solver {
  private var neverInitialized = true

  var ctx: Z3Context = null
  var z3Solver: Z3Solver = null
  var z3Vars: Map[Identifier, Z3AST] = null

  private val USEBV = opts.bitvectorWidth.isDefined
  private val BVWIDTH = opts.bitvectorWidth.getOrElse(-1)

  /** Restarts by instantiating a new context. */
  protected def restart(): Unit = {
    if (neverInitialized) {
      neverInitialized = false
    } else {
      ctx.delete
    }

    ctx = new Z3Context("MODEL" -> true)
    z3Solver = ctx.mkSolver()
    z3Vars = Map.empty
  }

  private def graphInterpretationConsistency(
      g: SymbolicGraph,
      i: SymbolicInterpretation): Expr = {
    if (opts.constraintOptions.temporality) {
      i.consistencyWithGraph(g)
    } else {
      BooleanLiteral(true)
    }
  }

  protected def createSymbolicGraphInterpretation(): 
      (SymbolicGraph, SymbolicInterpretation, Expr) = {
    val symbolicGraph = new SymbolicGraph(???, ???, ???)
    val symbolicInterpretation = interpretation.makeSymbolic()

    val validModel = And(
      symbolicGraph.consistency(),
      symbolicGraph.properties(),
      symbolicInterpretation.consistency(),
      graphInterpretationConsistency(symbolicGraph, symbolicInterpretation)
    )

    (symbolicGraph, symbolicInterpretation, validModel)
  }

  private def edgeSolutionChoices: Set[EdgeSolution] = {
    activeEdgeSolutionChoices + InactiveEdge
  }

  protected def activeEdgeSolutionChoices: Set[EdgeSolution] = {
    for {
      d <- Set(Forward, Backward)
      s <- Set(Activating, Inhibiting)
    } yield {
      ActiveEdge(d, s)
    }
  }

  def assertExpr(expr: Expr) {
    z3Solver.assertCnstr(toZ3Formula(expr))
  }

  def check(): Option[Map[Identifier, Expr]] = {
    z3Solver.check() match {
      case Some(true) => Some(modelToMap(z3Solver.getModel()))
      case _ => None
    }
  }

  private def modelToMap(m: Z3Model): Map[Identifier, Expr] = {
    var model: Map[Identifier, Expr] = Map.empty
    for ((id, ast) <- z3Vars) {
      id.getType match {
        case Untyped => LogUtils.terminate("Untyped expr: " + id)
        case IntType => m.evalAs[Int](ast) match {
          case Some(v) => 
            model += ((id, IntLiteral(v)))
          case None =>
        }
        case BooleanType => m.evalAs[Boolean](ast) match {
          case Some(v) =>
            model += ((id, BooleanLiteral(v)))
          case None =>
        }
      }
    }
    model
  }

  def intSort: Z3Sort = {
    if (USEBV)
      ctx.mkBVSort(BVWIDTH)
    else
      ctx.mkIntSort
  }

  def booleanSort: Z3Sort = ctx.mkBoolSort

  private def typeToSort(tt: TypeTree): Z3Sort = tt match {
    case Untyped => LogUtils.terminate("Translating untyped expression")
    case IntType => intSort
    case BooleanType => booleanSort
  }

  private def toZ3Formula(expr: Expr): Z3AST = expr match {
    case And(exprs) => ctx.mkAnd(exprs.map(toZ3Formula(_)): _*)
    case Or(exprs) => ctx.mkOr(exprs.map(toZ3Formula(_)): _*)
    case Iff(l, r) => ctx.mkIff(toZ3Formula(l), toZ3Formula(r))
    case Implies(l, r) => ctx.mkImplies(toZ3Formula(l), toZ3Formula(r))
    case Not(e) => ctx.mkNot(toZ3Formula(e))
    case Equals(l, r) => ctx.mkEq(toZ3Formula(l), toZ3Formula(r))
    case Plus(l, r) => 
      val nl = toZ3Formula(l)
      val nr = toZ3Formula(r)
      if (USEBV) ctx.mkBVAdd(nl, nr) else ctx.mkAdd(nl, nr)
    case LessThan(l, r) => 
      val nl = toZ3Formula(l)
      val nr = toZ3Formula(r)
      if (USEBV) ctx.mkBVSlt(nl, nr) else ctx.mkLT(nl, nr)
    case GreaterThan(l, r) =>
      val nl = toZ3Formula(l)
      val nr = toZ3Formula(r)
      if (USEBV) ctx.mkBVSgt(nl, nr) else ctx.mkGT(nl, nr)
    case LessEquals(l, r) =>
      val nl = toZ3Formula(l)
      val nr = toZ3Formula(r)
      if (USEBV) ctx.mkBVSle(nl, nr) else ctx.mkLE(nl, nr)
    case GreaterEquals(l, r) =>
      val nl = toZ3Formula(l)
      val nr = toZ3Formula(r)
      if (USEBV) ctx.mkBVSge(nl, nr) else ctx.mkGE(nl, nr)
    case v @ Variable(id) => z3Vars.get(id) match {
      case Some(ast) => ast
      case None =>
        val newAST = ctx.mkFreshConst(id.uniqueName, typeToSort(v.getType))
        z3Vars = z3Vars + (id -> newAST)
        newAST
    }
    case IntLiteral(v) => ctx.mkInt(v, intSort)
    case BooleanLiteral(v) => if (v) ctx.mkTrue else ctx.mkFalse
  }

}
