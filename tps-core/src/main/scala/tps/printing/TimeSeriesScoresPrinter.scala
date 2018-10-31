package tps.printing

import tps.TabularData

object TimeSeriesScoresPrinter {

  def apply(scores: Map[String, Seq[Double]]): String = {
    val tuples = scores.toSeq map {
      case (id, vs) => id +: vs.map(_.toString())
    }
    TabularData(Nil, tuples).toTSVString(printHeaders = false)
  }

}
