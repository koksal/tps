package tps.simulation

import breeze.stats.distributions.Poisson
import tps.Graphs.UndirectedGraph
import tps.PeptideExpansion.PeptideProteinMap
import tps.{Profile, TimeSeries}

/**
  * Generates random time series data for a given [[tps.Graphs.UndirectedGraph]]
  */
object RandomTimeSeriesGenerator {

  private val RANDOM_SEED = 2483967
  private val random = new scala.util.Random(RANDOM_SEED)

  private val MAX_PROFILE_VALUE = 10

  private val NB_TIME_POINTS = 8

  // Poisson parameter for drawing number of phosphosites
  private val NB_SITES_POISSON_PARAMETER = 1.7
  private val nbSitesDistribution = Poisson.distribution(
    NB_SITES_POISSON_PARAMETER)

  private val NB_SIGNIFICANT_TIME_POINTS_POISSON_PARAMETER = 0.55
  private val nbSignificantTimePointsDistribution = Poisson.distribution(
    NB_SIGNIFICANT_TIME_POINTS_POISSON_PARAMETER)

  def generateRandomPeptideProteinMap(
    graph: UndirectedGraph
  ): PeptideProteinMap = {
    var mapping: PeptideProteinMap = Map.empty
    for (v <- graph.V) {
      val nbSites = nbSitesDistribution.draw()
      for (i <- 0 until nbSites) {
        val siteName = s"${v.id}#site${i}"
        assert(!mapping.isDefinedAt(siteName))
        mapping += siteName -> Set(v.id)
      }
    }
    mapping
  }

  def generateRandomTimeSeries(pepIDs: Set[String]): TimeSeries = {
    val profiles = pepIDs.toSeq map { id =>
      generateProfile(id)
    }
    TimeSeries(generateLabels(), profiles)
  }

  /**
    * Generates random significance scores for the given time series data.
    */
  def generateSignificanceScores(
    timeSeries: TimeSeries
  ): Map[String, Seq[Double]] = {
    val pairs = for (p <- timeSeries.profiles) yield {
      // decide how many points should be significant
      val nbSignificantTimePoints = nbSignificantTimePointsDistribution.draw()
      val significanceProb = nbSignificantTimePoints.toDouble / NB_TIME_POINTS

      // compute scores for all time points except the first
      val sigScores = p.values.tail.map { v =>
        if (random.nextDouble() < significanceProb) 0.0 else 1.0
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
