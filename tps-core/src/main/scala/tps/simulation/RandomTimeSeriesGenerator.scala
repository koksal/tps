package tps.simulation

import tps.Graphs.UndirectedGraph
import tps.{Profile, TimeSeries}

/**
  * Generates random time series data for a given [[tps.Graphs.UndirectedGraph]]
  */
object RandomTimeSeriesGenerator {

  private val RANDOM_SEED = 2483967
  private val random = new scala.util.Random(RANDOM_SEED)

  private val MAX_PROFILE_VALUE = 10

  private val NB_TIME_POINTS = 10

  // probability of a node having time series data
  private val COVERAGE_RATIO = 0.6

  // probability of a measurement being significaint
  private val SIGNIFICANCE_RATIO = 0.2

  /**
    * Generates random time series data for the given graph.
    *
    * Data is generated for a subset of the nodes in the graph.
    */
  def generateRandomTimeSeries(graph: UndirectedGraph): TimeSeries = {
    var profiles: Set[Profile] = Set.empty
    for (v <- graph.V) {
      if (random.nextDouble() < COVERAGE_RATIO) {
        profiles += generateProfile(v.id)
      }
    }
    TimeSeries(generateLabels(), profiles.toSeq)
  }

  /**
    * Generates random significance scores for the given time series data.
    */
  def generateSignificanceScores(
    timeSeries: TimeSeries
  ): Map[String, Seq[Double]] = {
    val pairs = for (p <- timeSeries.profiles) yield {
      // compute scores for all time points except the first
      val sigScores = p.values.tail.map { v =>
        if (random.nextDouble() < SIGNIFICANCE_RATIO) 0.0 else 1.0
      }
      p.id -> sigScores
    }
    pairs.toMap
  }

  private def generateLabels(): Seq[String] = {
    (1 to NB_TIME_POINTS).map(i => s"t$i")
  }

  private def generateProfile(id: String): Profile = {
    val values = (0 until NB_TIME_POINTS) map { i =>
      Some(random.nextDouble() * MAX_PROFILE_VALUE)
    }
    Profile(id, values)
  }

}
