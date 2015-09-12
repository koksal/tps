package tps.synthesis

import scala.collection.mutable.{Map => MutableMap}

import Trees._
import TreeOps._
import TypeTrees._

import tps.UndirectedGraphs._
import tps.GraphSolutions._

import tps.util.LogUtils

class SymbolicGraph(
  graph: UndirectedGraph, 
  partialModel: AmbiguousGraphSolution, 
  opts: SynthesisOptions
) {

  /** isActivatorVars(v1)(v2) iff v2 activates v1 */
  val isActivatorVars: MutableMap[Vertex, MutableMap[Vertex, Variable]] = 
    MutableMap.empty

  /** isActivateeVars(v1)(v2) iff v2 is activated by v1 */
  val isActivateeVars: MutableMap[Vertex, MutableMap[Vertex, Variable]] = 
    MutableMap.empty

  /** isInhibitorVars(v1)(v2) iff v2 inhibits v1 */
  val isInhibitorVars: MutableMap[Vertex, MutableMap[Vertex, Variable]] =
    MutableMap.empty

  /** isInhibiteeVars(v1)(v2) iff v2 is inhibited by v1 */
  val isInhibiteeVars: MutableMap[Vertex, MutableMap[Vertex, Variable]] =
    MutableMap.empty

  /** isInactiveVars(v1)(v2) iff edge (v1, v2) is inactive */
  val isInactiveVars: MutableMap[Vertex, MutableMap[Vertex, Variable]] = 
    MutableMap.empty

  private def createMapOrUpdate(
    m: MutableMap[Vertex, MutableMap[Vertex, Variable]],
    v1: Vertex, v2: Vertex, variable: Variable) = {
    m.get(v1) match {
      case Some(m2) => m2 += ((v2, variable))
      case None => m += ((v1, MutableMap(v2 -> variable)))
    }
  }

  private def createEdgeVars(v1: Vertex, v2: Vertex): Unit = {
      val newActivatorVar = FreshIdentifier(
        v1 + "_" + v2 + "_" + "activator").setType(BooleanType).toVariable
      val newActivateeVar = FreshIdentifier(
        v1 + "_" + v2 + "_" + "activatee").setType(BooleanType).toVariable
      val newInhibitorVar = FreshIdentifier(
        v1 + "_" + v2 + "_" + "inhibitor").setType(BooleanType).toVariable
      val newInhibiteeVar = FreshIdentifier(
        v1 + "_" + v2 + "_" + "inhibitee").setType(BooleanType).toVariable
      val newInactiveVar  = FreshIdentifier(
        v1 + "_" + v2 + "_" + "inactive").setType(BooleanType).toVariable
      
      createMapOrUpdate(isActivatorVars, v1, v2, newActivatorVar)
      createMapOrUpdate(isActivateeVars, v1, v2, newActivateeVar)
      createMapOrUpdate(isInhibitorVars, v1, v2, newInhibitorVar)
      createMapOrUpdate(isInhibiteeVars, v1, v2, newInhibiteeVar)
      createMapOrUpdate(isInactiveVars, v1, v2, newInactiveVar)
  }

  /** Only creates the symbolic edge variables. */
  private def prepareEdgeVars(): Unit = {
    for (Edge(v1, v2) <- graph.E) {
      createEdgeVars(v1, v2)
      createEdgeVars(v2, v1)
    }
  }

  /** Builds a consistency constraint on edge variables of the graph. */
  private def edgeVarConsistency(): Expr = {
    var consistency: Expr = BooleanLiteral(true)

    for (Edge(v1, v2) <- graph.E) {
      val edgeConsistency = And(
        onlyOne(
          isActivatorVars(v1)(v2), 
          isActivateeVars(v1)(v2), 
          isInhibitorVars(v1)(v2), 
          isInhibiteeVars(v1)(v2), 
          isInactiveVars(v1)(v2)
        ),
        onlyOne(
          isActivatorVars(v2)(v1), 
          isActivateeVars(v2)(v1), 
          isInhibitorVars(v2)(v1), 
          isInhibiteeVars(v2)(v1), 
          isInactiveVars(v2)(v1)
        ),
        Iff(isActivatorVars(v1)(v2), isActivateeVars(v2)(v1)),
        Iff(isActivateeVars(v1)(v2), isActivatorVars(v2)(v1)),
        Iff(isInhibitorVars(v1)(v2), isInhibiteeVars(v2)(v1)),
        Iff(isInhibiteeVars(v1)(v2), isInhibitorVars(v2)(v1)),
        Iff(isInactiveVars(v1)(v2), isInactiveVars(v2)(v1))
      )
      consistency = And(consistency, edgeConsistency)
    }

    consistency
  }

  /** Each non-source vertex has an activator if it activates or inhibits another. */
  private def connectivity(): Expr = {
    var constraint: Expr = BooleanLiteral(true)
    val nonSourceVertices = graph.V -- graph.sources
    
    for (v <- nonSourceVertices) {
      val vActivatorVars = isActivatorVars(v).values.toSeq
      val vInhibitorVars = isInhibitorVars(v).values.toSeq
      val vActivateeVars = isActivateeVars(v).values.toSeq
      val vInhibiteeVars = isInhibiteeVars(v).values.toSeq
      val vHasInput = Or((vActivatorVars ++ vInhibitorVars): _*)
      val vHasOutput = Or(vActivateeVars ++ vInhibiteeVars: _*)

      constraint = And(constraint, Implies(vHasOutput, vHasInput))
    }

    constraint
  }

  /** Each path to a node n is no longer than the shortest distance to n from a source */
  private def pathLenSlack(k: Int): Expr = {
    val pathLenVars: Map[Vertex, Variable] = graph.V.map{ v =>
      v -> mkFreshIntVar(v.id + "-pathlen")
    }.toMap

    // path lengths are non-negative or -1
    val inRange = graph.V.map{ v =>
      GreaterEquals(pathLenVars(v), IntLiteral(-1))
    }

    // origins have distance 0
    val originDist = graph.sources.map{ v =>
      Equals(pathLenVars(v), IntLiteral(0))
    }

    // path length increase for each edge
    val pathLenIncrease = graph.E.map{ case Edge(v1, v2) =>
      val v1Affectsv2 = Or(
        isActivatorVars(v2)(v1),
        isInhibitorVars(v2)(v1)
      )
      val v2Affectsv1 = Or(
        isActivatorVars(v1)(v2),
        isInhibitorVars(v1)(v2)
      )
      val leftToRight = Implies(
        v1Affectsv2,
        GreaterThan(pathLenVars(v2), pathLenVars(v1))
      )
      val rightToLeft = Implies(
        v2Affectsv1,
        GreaterThan(pathLenVars(v1), pathLenVars(v2))
      )
      And(leftToRight, rightToLeft)
    }

    val shortestDist = graph.shortestDistances(graph.sources)

    // path lengths are at most k more than shortest path
    val atMostK = graph.V.map{ v =>
      LessEquals(pathLenVars(v), IntLiteral(shortestDist(v) + k))
    }

    // scalability: assert shortest distance to each node as lower bound
    val distLowerBounds = graph.V.map{ v =>
      Or(
        GreaterEquals(pathLenVars(v), IntLiteral(shortestDist(v))),
        Equals(pathLenVars(v), IntLiteral(-1))
      )
    }

    And(
      And(inRange.toList: _*),
      And(atMostK.toList: _*),
      And(pathLenIncrease.toList: _*),
      And(originDist.toList: _*),
      And(distLowerBounds.toList: _*)
    )
  }

  private def isTree(): Expr = {
    var constraint: Expr = BooleanLiteral(true)
    
    for (v <- graph.V) {
      val vActivators = isActivatorVars(v).values.toSeq
      val vInhibitors = isInhibitorVars(v).values.toSeq
      val atMostOnePredecessor = atMostOne(vActivators ++ vInhibitors: _*)
      constraint = And(constraint, atMostOnePredecessor)
    }

    constraint
  }

  /** Builds the constraint on consistency of variables encoding edge
   *  directions and number of active edges.
   *
   *  All instantiations of symbolic graphs should be consistent (we don't
   *  want to end up with malformed symbolic graphs).
   */
  def consistency(): Expr = {
    edgeVarConsistency()
  }

  /** Builds a constraint that expresses that the graph is connected,
   *  satisfies the partial solution, and the number of active edges is 
   *  within bounds specified in the configuration.
   *
   *  This method is decoupled from the consistency method, because we
   *  desire to express a graph that is consistent but doesn't satisfy
   *  the connectivity, partial solution and edge number properties.
   */
  def properties(): Expr = {
    val conn = if (opts.constraintOptions.connectivity) connectivity() 
               else BooleanLiteral(true)

    val limitPathLength = true

    val pathLenCnstr = opts.pathLengthSlack match {
      case None => BooleanLiteral(true)
      case Some(k) => pathLenSlack(k)
    }
    
    And(
      conn,
      graphSolutionFormula(partialModel),
      pathLenCnstr,
      noEdgeToSources
    )
  }

  private def noEdgeToSources(): Expr = {
    var constraint: Expr = BooleanLiteral(true)

    for (sv <- graph.sources) {
      val svActivators = isActivatorVars(sv).values.toSeq
      val svInhibitors = isInhibitorVars(sv).values.toSeq
      constraint = And(
        constraint, 
        Not(Or(svActivators ++ svInhibitors: _*))
      )
    }

    constraint
  }

  /** Builds a formula that encodes the edge solution. */
  def graphSolutionFormula(sol: AmbiguousGraphSolution): Expr = {
    val conjuncts = for ((Edge(v1, v2), edgeSolSet) <- sol) yield {
      assert(!edgeSolSet.isEmpty)
      val possibilities = for (edgeSol <- edgeSolSet) yield {
        edgeSol match {
          case InactiveEdge => isInactiveVars(v1)(v2)
          case ActiveEdge(dir, sign) => {
            (dir, sign) match {
              case (Forward, Activating) => isActivateeVars(v1)(v2)
              case (Backward, Activating) => isActivatorVars(v1)(v2)
              case (Forward, Inhibiting) => isInhibiteeVars(v1)(v2)
              case (Backward, Inhibiting) => isInhibitorVars(v1)(v2)
            }
          }
        }
      }
      Or(possibilities.toSeq: _*)
    }

    And(conjuncts.toSeq: _*)
  }

  /** Recover edge directionality from model */
  def solution(m: Map[Identifier, Expr]): AmbiguousGraphSolution = {
    val tuples = for (e @ Edge(v1, v2) <- graph.E) yield {
      val isActivatorEval = m(isActivatorVars(v1)(v2).id)
      val isActivateeEval = m(isActivateeVars(v1)(v2).id)
      val isInhibitorEval = m(isInhibitorVars(v1)(v2).id)
      val isInhibiteeEval = m(isInhibiteeVars(v1)(v2).id)
      val isInactiveEval  = m(isInactiveVars(v1)(v2).id)
      
      (isActivatorEval, isActivateeEval, 
          isInhibitorEval, isInhibiteeEval, isInactiveEval) match {
        case (BooleanLiteral(activatorV), BooleanLiteral(activateeV), 
              BooleanLiteral(inhibitorV), BooleanLiteral(inhibiteeV),
              BooleanLiteral(inactiveV)) =>
          if (activatorV) {
            assert(!activateeV && !inhibitorV && !inhibiteeV && !inactiveV)
            (e, ActiveEdge(Backward, Activating))
          } else if (activateeV) {
            assert(!inhibitorV && !inhibiteeV && !inactiveV)
            (e, ActiveEdge(Forward, Activating))
          } else if (inhibitorV) {
            assert(!inhibiteeV && !inactiveV)
            (e, ActiveEdge(Backward, Inhibiting))
          } else if (inhibiteeV) {
            assert(!inactiveV)
            (e, ActiveEdge(Forward, Inhibiting))
          } else {
            (e, InactiveEdge)
          }
        case _ => LogUtils.terminate("Type mismatch.")
      }
    }
    tuples.map{ case (e, ae) => e -> Set[EdgeSolution](ae) }.toMap
  }

  private def prepareVars(): Unit = {
    prepareEdgeVars()
  }

  prepareVars()
}
