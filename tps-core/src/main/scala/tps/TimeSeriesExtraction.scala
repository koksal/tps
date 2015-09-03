package tps

import tps.util.FileUtils._
import tps.util.LogUtils._

import java.io.File

object TimeSeriesExtraction {
  def run(f: File): TimeSeries =  {
    val data = new TSVSource(f).data
    val timeLabels = data.fields.tail
    val profiles = data.tuples map { tuple =>
      val Seq(profileID, values @ _*) = tuple
      Profile(profileID, values map (_.toDouble))
    }

    TimeSeries(timeLabels, profiles.toSet)
  }
}
