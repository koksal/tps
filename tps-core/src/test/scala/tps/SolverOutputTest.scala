package tps
package test

import org.scalatest.FunSuite
import org.scalatest.Matchers

import Graphs._
import tps.synthesis.SynthesisOptions

import java.io.File

class SolverOutputTest extends FunSuite with Matchers {

  private val resourceBaseFolder = "/simple-example"
  private def testFile(name: String): File = {
    val url = getClass.getResource(s"$resourceBaseFolder/$name")
    new File(url.getFile())
  }

  test("output of solvers match") {
    val network = NetworkExtraction.run(testFile("network.tsv"))
    val timeSeries = TimeSeriesExtraction.run(testFile("time-series.tsv"))
    val firstScores = TimeSeriesScoresExtraction.run(testFile("significance-first.tsv"))
    val prevScores  = TimeSeriesScoresExtraction.run(testFile("significance-prev.tsv"))
    val partialModel = PartialModelExtraction.run(testFile("partial-model.sif"))
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
