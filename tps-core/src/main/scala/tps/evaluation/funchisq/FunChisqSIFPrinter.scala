package tps.evaluation.funchisq

import tps.Graphs.Backward
import tps.Graphs.Forward
import tps.evaluation.funchisq.FunChisqGraphs.FunChisqGraph

object FunChisqSIFPrinter {
  def print(fcg: FunChisqGraph): String = {
    val sb = new StringBuilder()

    for ((edge, (edgeDir, _)) <- fcg) {
      val rel = "N"
      val (src, tgt) = edgeDir match {
        case Forward => (edge.v1.id, edge.v2.id)
        case Backward => (edge.v2.id, edge.v1.id)
      }

      sb append List(src, rel, tgt).mkString("\t")
      sb append "\n"
    }

    sb.toString()
  }
}
