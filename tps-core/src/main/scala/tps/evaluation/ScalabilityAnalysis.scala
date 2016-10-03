package tps.evaluation

import tps._
import tps.simulation.{RandomGraphGenerator, RandomTimeSeriesGenerator}
import tps.synthesis.{Synthesis, SynthesisOptions}
import tps.util.{Stopwatch, TimingUtil}

/**
  * Evaluates solver running time on randomly simulated data.
  */
object ScalabilityAnalysis {

  private val MIN_GRAPH_SIZE = 1000
  private val MAX_GRAPH_SIZE = 100000

  private val SIGNIFICANCE_THRESHOLD = 0.01

  def main(args: Array[String]): Unit = {
    val resultReporter = new NoopReporter()

    var size = MIN_GRAPH_SIZE
    do {
      println(s"Evaluating with size $size")

      TimingUtil.timeReplicates(s"Scalability analysis for $size", 5) {
        val g = RandomGraphGenerator.generateRandomGraph(size)
        val ppm = RandomTimeSeriesGenerator.generateRandomPeptideProteinMap(g)
        val ts = RandomTimeSeriesGenerator.generateRandomTimeSeries(ppm.keySet)
        val scores = RandomTimeSeriesGenerator.generateSignificanceScores(ts)

        Synthesis.run(
          g,
          ts,
          scores,
          scores,
          Set.empty,
          Map.empty,
          g.sources map (_.id),
          SIGNIFICANCE_THRESHOLD,
          SynthesisOptions(),
          resultReporter
        )

        println("Nb. phosphosites: " + ppm.keySet.size)
      }
      
      size = size * 2
    } while (size <= MAX_GRAPH_SIZE)
  }
}
