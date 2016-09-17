package tps

import tps.util.FileUtils._
import tps.util.LogUtils._

import java.io.File

object TimeSeriesParser {
  def run(f: File): TimeSeries =  {
    val data = new TSVSource(f).data
    val timeLabels = data.fields.tail
    val profiles = data.tuples map { tuple =>
      val Seq(profileID, values @ _*) = tuple
      Profile(profileID, values map parseOptDouble)
    }

    TimeSeries(timeLabels, profiles)
  }

  private def parseOptDouble(s: String): Option[Double] = {
    try {
      Some(s.toDouble)
    } catch {
      case e: NumberFormatException => s match {
        case _ => None
      }
    }
  }
}
