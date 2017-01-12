package tps.evaluation

import java.io.File

import tps.Profile
import tps.TabularData
import tps.TimeSeries
import tps.TimeSeriesParser

/**
  * Generates significance score files based on fold change thresholding from
  * a time series file with a single replicate.
  */
object SignificanceScoreProducer {

  val SIGNIFICANT_SCORE = 0.0
  val NON_SIGNIFICANT_SCORE = 1.0

  def main(args: Array[String]): Unit = {
    val timeSeriesFile = new File(args(0))
    val foldChangeThreshold = args(1).toDouble
    val timeSeries = TimeSeriesParser.run(timeSeriesFile)

    val prevScoresStr = prevScores(
      timeSeries, foldChangeThreshold).toTSVString()

    println(prevScoresStr)
  }

  def prevScores(ts: TimeSeries, foldChangeThreshold: Double): TabularData = {
    val baselineLabel = ts.labels.head
    val restLabels = ts.labels.tail
    val scoreLabels = restLabels map { l =>
      s"$l vs. $baselineLabel"
    }
    val finalLabels: Seq[String] = "id" +: scoreLabels
    val scoresPerProfile = ts.profiles map {
      case Profile(id, vs) => {
        val baseline = vs.head.get
        val restValues = vs.tail
        val foldChanges = restValues map (vOpt => vOpt.get / baseline)
        val scores: Seq[String] = foldChanges map { fc =>
          if (fc >= foldChangeThreshold) {
            SIGNIFICANT_SCORE.toString
          } else {
            NON_SIGNIFICANT_SCORE.toString
          }
        }
        id +: scores
      }
    }

    TabularData(finalLabels, scoresPerProfile)
  }

}
