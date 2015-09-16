package tps.synthesis

import tps.Graphs._

import tps.util.LogUtils

import Trees._

class BilateralSolver(
  graph: UndirectedGraph,
  partialModel: SignedDirectedGraph,
  opts: SynthesisOptions,
  interpretation: Interpretation
) extends AbstractSymbolicSolver(graph, partialModel, opts, interpretation) {

  private def emptySol(g: UndirectedGraph): SignedDirectedGraph = {
    g.E.map{ e => e -> Set[SignedDirectedEdgeLabel]() }.toMap
  }

  private def fullSol(g: UndirectedGraph): SignedDirectedGraph = {
    g.E.map{ e => e -> activeEdgeLabelChoices }.toMap
  }

  private def abstractConsequence(
    lower: SignedDirectedGraph,
    upper: SignedDirectedGraph,
    bfsEdgeOrder: Seq[Edge]
  ): SignedDirectedGraph = {
    bfsEdgeOrder.find{ e =>
      val lowerSet = lower(e)
      val upperSet = upper(e)
      val isSubset = lowerSet subsetOf upperSet
      if (!isSubset) {
        throw new Exception(s"e: $e, lower: $lowerSet, upper: $upperSet")
      }
      upperSet != lowerSet
    } match {
      case Some(e) => Map(e -> lower(e))
      case None => throw new Exception("should not happen")
    }
  }

  private def bilateralCheck(
    phi: Expr,
    q: SignedDirectedGraph,
    upper: SignedDirectedGraph,
    sg: SymbolicGraph
  ): Option[SignedDirectedGraph] = {
    // three ways to restart:
    restart()
    // z3Solver.reset()
    // z3Solver = ctx.mkSolver()

    assertExpr(phi)

    val qWithInactive = q map { case (e, ess) =>
      e -> (ess + InactiveEdge)
    }
    val qFormula = sg.graphSolutionFormula(qWithInactive)
    assertExpr(Not(qFormula))
    
    val upperWithInactive = upper map { case (e, ess) =>
      e -> (ess + InactiveEdge)
    }
    val upperFormula = sg.graphSolutionFormula(upperWithInactive)
    assertExpr(upperFormula)

    val res = check() map { model =>
      sg.solution(model)
    }

    res map { graphSol =>
      graphSol map { case (e, ess) =>
        e -> (ess - InactiveEdge)
      }
    }
  }

  private def meet(
    s1: SignedDirectedGraph, 
    s2: SignedDirectedGraph
  ): SignedDirectedGraph = {
    val common = s1.keySet intersect s2.keySet
    val met = common map { e =>
      e -> (s1(e) intersect s2(e))
    }
    s1 ++ s2 ++ met
  }

  private def join(
    s1: SignedDirectedGraph, 
    s2: SignedDirectedGraph
  ): SignedDirectedGraph = {
    val common = s1.keySet intersect s2.keySet
    val joined = common map { e =>
      e -> (s1(e) union s2(e))
    }
    s1 ++ s2 ++ joined
  }

  def summary(): SignedDirectedGraph = {
    restart()
    val (sg, si, phi) = createSymbolicGraphInterpretation()

    val undirGraph = graph
    val bfsEdgeOrder: Seq[Edge] = undirGraph.bfsEdgeOrder
    var lower = emptySol(undirGraph)
    var upper = fullSol(undirGraph)

    LogUtils.log("|V| = " + undirGraph.V.size)
    LogUtils.log("|E| = " + undirGraph.E.size)

    var ctr = 0
    var nbSwitches = 0

    sealed trait AbsType
    case object Lower extends AbsType
    case object Upper extends AbsType
    var lastUpdatedAbs: AbsType = Lower

    val rsrcSW = new tps.util.Stopwatch("Bilateral resource", false).start
    def queryLimitReached(): Boolean = {
      opts.bilateralTimeout match {
        case None => false
        case Some(lim) => rsrcSW.elapsed > lim.toDouble
      }
    }

    while (lower != upper && !queryLimitReached()) {
      ctr += 1
      val q = abstractConsequence(lower, upper, bfsEdgeOrder)
      bilateralCheck(phi, q, upper, sg) match {
        case None =>
          val oldUpper = upper
          upper = meet(upper, q)
          if (oldUpper == upper) throw new Exception("upper not changed!")
          if (lastUpdatedAbs != Upper) nbSwitches += 1
          lastUpdatedAbs = Upper
        case Some(graphSol) =>
          val oldLower = lower
          lower = join(lower, graphSol)
          if (oldLower == lower) throw new Exception("lower not changed!")
          if (lastUpdatedAbs != Lower) nbSwitches += 1
          lastUpdatedAbs = Lower
      }
    }
    LogUtils.log("# queries: " + ctr)
    LogUtils.log("# switches: " + nbSwitches)
    rsrcSW.stop

    upper
  }
}
