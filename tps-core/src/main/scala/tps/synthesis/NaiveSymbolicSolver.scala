package tps.synthesis

import tps.UndirectedGraphs._
import tps.GraphSolutions._

import tps.util.LogUtils

class NaiveSymbolicSolver(
  graph: UndirectedGraph,
  opts: SynthesisOptions,
  interpretation: Interpretation
) extends AbstractSymbolicSolver(opts, interpretation) {
  private def testSolution(
    sg: SymbolicGraph, 
    si: SymbolicInterpretation, 
    e: Edge, 
    es: EdgeSolution
  ): Boolean = {
    z3Solver.push

    val toCheck = Map(e -> Set(es))
    val formulaToCheck = sg.graphSolutionFormula(toCheck)
    assertExpr(formulaToCheck)

    val sw = new tps.util.Stopwatch(s"Testing: $e for $es", false).start
    val result = z3Solver.check match {
      case Some(true) => true
      case _ => false
    }
    sw.stop

    z3Solver.pop(1)

    result
  }

  def summary(): AmbiguousGraphSolution = {
    restart()
    val (sg, si, validModelFla) = createSymbolicGraphInterpretation()
    assertExpr(validModelFla)

    var solution: AmbiguousGraphSolution = Map.empty

    val total = graph.E.size
    LogUtils.log(s"$total edges to test")
    var ctr = 0

    for (e <- graph.bfsEdgeOrder) {
      var possibleSols = activeEdgeSolutionChoices filter {
        es => ctr += 1; testSolution(sg, si, e, es)
      }
      possibleSols += InactiveEdge

      val newPartialModel = Map(e -> possibleSols)
      solution ++= newPartialModel

      // assert what's already known
      assertExpr(sg.graphSolutionFormula(newPartialModel))
    }
    LogUtils.log("# queries: " + ctr)

    solution
  }
}
