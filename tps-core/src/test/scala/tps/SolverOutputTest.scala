package tps

import org.scalatest.FunSuite
import org.scalatest.Matchers

import Graphs._
import tps.synthesis.SynthesisOptions

import TestResourceUtil.testFile

class SolverOutputTest extends FunSuite with Matchers {

  test("output of solvers match") {
    val network = UndirectedGraphParser.run(testFile("network.tsv"))
    val timeSeries = TimeSeriesParser.run(testFile("time-series.tsv"))
    val firstScores = TimeSeriesScoresParser.run(testFile("significance-first.tsv"))
    val prevScores  = TimeSeriesScoresParser.run(testFile("significance-prev.tsv"))
    val partialModel = SignedDirectedGraphParser.run(testFile("partial-model.sif"))
    val ppm: Map[String, Set[String]] = Map.empty
    val sources = Set("A")
    val significanceThreshold = 0.05
    val synOpts = synthesis.SynthesisOptions()
    val reporter = new NoopReporter()

    def runSolver(so: SynthesisOptions): SignedDirectedGraph = {
      synthesis.Synthesis.run(
        network,
        timeSeries,
        firstScores,
        prevScores,
        Set(partialModel),
        ppm,
        sources,
        significanceThreshold,
        so,
        reporter
      )
    }

    val dataflowOutput = runSolver(synOpts)
    val naiveSymbolicOutput = runSolver(synOpts.copy(solver = "naive"))
    val bilateralSymbolicOutput = runSolver(synOpts.copy(solver = "bilateral"))

    val expectedNetwork = Map(
      // missing B data, direction from topology
      Edge(Vertex("A"), Vertex("B")) -> Set(
        ActiveEdge(Forward, Activating),
        ActiveEdge(Forward, Inhibiting)
      ),
      // missing B data, direction from topology
      Edge(Vertex("B"), Vertex("C")) -> Set(
        ActiveEdge(Forward, Activating),
        ActiveEdge(Forward, Inhibiting)
      ),
      // missing B data, tree constraint dictates one direction
      Edge(Vertex("B"), Vertex("E")) -> Set(
        ActiveEdge(Forward, Activating),
        ActiveEdge(Forward, Inhibiting)
      ),
      // partial model + data + tree
      Edge(Vertex("C"), Vertex("D")) -> Set(
        ActiveEdge(Forward, Inhibiting)
      ),
      // both directions are possible
      Edge(Vertex("C"), Vertex("E")) -> Set(
        ActiveEdge(Forward, Inhibiting),
        ActiveEdge(Backward, Inhibiting)
      ),
      // either F activates D or D inhibits F
      Edge(Vertex("D"), Vertex("F")) -> Set(
        ActiveEdge(Forward, Inhibiting),
        ActiveEdge(Backward, Activating)
      ),
      // data on D rules out backward direction
      Edge(Vertex("E"), Vertex("F")) -> Set(
        ActiveEdge(Forward, Activating),
        ActiveEdge(Forward, Inhibiting)
      )
    )

    dataflowOutput should equal (expectedNetwork)
    naiveSymbolicOutput should equal (expectedNetwork)
    bilateralSymbolicOutput should equal (expectedNetwork)
    
  }
}
