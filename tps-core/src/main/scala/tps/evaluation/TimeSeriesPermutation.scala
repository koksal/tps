package tps.evaluation

import java.io.File

import tps.TimeSeriesParser

object TimeSeriesPermutation {
  def main(args: Array[String]): Unit = {
    val timeSeriesFile = new File(args(0))
    val timeSeries = TimeSeriesParser.run(timeSeriesFile)

    // permute and print
  }
}
