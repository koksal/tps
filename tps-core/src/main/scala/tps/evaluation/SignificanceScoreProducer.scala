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

    val firstScoresStr = firstScores(
      timeSeries, foldChangeThreshold).toTSVString()
    val prevScoresStr = prevScores(
      timeSeries, foldChangeThreshold).toTSVString()

    println(firstScoresStr)
    println(prevScoresStr)
  }

  def firstScores(ts: TimeSeries, foldChangeThreshold: Double): TabularData = {
    val baselineLabel = ts.labels.head
    val restLabels = ts.labels.tail
    val scoreLabels = restLabels map { l =>
      s"$l vs. $baselineLabel"
    }
    val finalLabels = "id" +: scoreLabels

    val scoresPerProfile = ts.profiles map {
      case Profile(id, vs) => {
        val baseline = vs.head.get
        val restValues = vs.tail
        val foldChanges = restValues map ( vOpt =>
          absLog2FoldChange(vOpt.get, baseline))
        id +: scoresFromFoldChanges(foldChanges, foldChangeThreshold)
      }
    }

    TabularData(finalLabels, scoresPerProfile)
  }

  def prevScores(ts: TimeSeries, foldChangeThreshold: Double): TabularData = {
    val scoreLabels = ts.labels.zip(ts.labels.tail).map {
      case (prevLabel, currLabel) =>
        s"$currLabel vs. $prevLabel"
    }
    val finalLabels = "id" +: scoreLabels

    val scoresPerProfile = ts.profiles map {
      case Profile(id, vs) => {
        val foldChanges = vs.zip(vs.tail).map {
          case (prevVal, currVal) => absLog2FoldChange(currVal.get, prevVal.get)
        }
        id +: scoresFromFoldChanges(foldChanges, foldChangeThreshold)
      }
    }

    TabularData(finalLabels, scoresPerProfile)
  }

  private def absLog2FoldChange(value: Double, baseline: Double): Double = {
    math.abs(math.log(value / baseline) / math.log(2))
  }

  private def scoresFromFoldChanges(
    foldChanges: Seq[Double], threshold: Double
  ): Seq[String] = {
    foldChanges map { fc =>
      if (fc >= threshold) {
        SIGNIFICANT_SCORE.toString
      } else {
        NON_SIGNIFICANT_SCORE.toString
      }
    }
  }
}
