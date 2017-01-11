package tps.evaluation.funchisq

import tps.evaluation.funchisq.FunChisqGraphs.FunChisqGraph

object FunChisqSIFPrinter {
  def print(fcg: FunChisqGraph): String = {
    val sb = new StringBuilder()

    for ((edge, _) <- fcg) {
      val rel = "N"

      sb append List(edge.v1.id, rel, edge.v2.id).mkString("\t")
      sb append "\n"
    }

    sb.toString()
  }
}
