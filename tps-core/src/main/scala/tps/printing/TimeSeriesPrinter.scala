package tps.printing

import tps.TabularData
import tps.TimeSeries

object TimeSeriesPrinter {
  private val ID_FIELD = "id"

  def apply(ts: TimeSeries): String = {
    val fields = ID_FIELD +: ts.labels
    val tuples = ts.profiles.map(p => p.id +: p.values.map(optDouble2String))

    TabularData(fields, tuples).toTSVString()
  }

  private def optDouble2String(dOpt: Option[Double]): String = dOpt match {
    case Some(v) => v.toString()
    case None => "NA"
  }
}
