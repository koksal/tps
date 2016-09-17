package tps.evaluation

import java.io.File

import tps.Graphs.UndirectedGraph
import tps._
import tps.simulation.{RandomGraphGenerator, RandomTimeSeriesGenerator}
import tps.synthesis.{Synthesis, SynthesisOptions}
import tps.util.Stopwatch

/**
  * Evaluates solver running time on randomly simulated data.
  */
object ScalabilityAnalysis {

  private val MIN_GRAPH_SIZE = 5000
  private val MAX_GRAPH_SIZE = 100000
  private val GRAPH_SIZE_STEP = 5000

  def main(args: Array[String]): Unit = {
    val resultReporter = new NoopReporter()

    val pin = PINParser.run(
      new File("data/networks/directed-pin-with-resource-edges.tsv"))
    val sourceGraph = UndirectedGraphOps.fromDirectedGraph(pin)
    val sources = Set("EGF_HUMAN")
    val threshold = 0.01

    for (size <- Range(MIN_GRAPH_SIZE, MAX_GRAPH_SIZE, GRAPH_SIZE_STEP)) {
      val g = RandomGraphGenerator.generateRandomGraph(sourceGraph, size)
      val ts = RandomTimeSeriesGenerator.generateRandomTimeSeries(g)
      val scores = RandomTimeSeriesGenerator.generateSignificanceScores(ts)

      val sw = new Stopwatch(s"Graph size: V = ${g.V.size}, E = ${g.E.size}",
        verbose = true)
      sw.start
      Synthesis.run(
        g,
        ts,
        scores,
        scores,
        Set.empty,
        Map.empty,
        sources,
        threshold,
        SynthesisOptions(),
        resultReporter
      )
      sw.stop
    }
  }
}
