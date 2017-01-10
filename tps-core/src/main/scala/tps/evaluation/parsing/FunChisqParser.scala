package tps.evaluation.parsing

import java.io.File

import tps.GraphParsing
import tps.TSVSource
import tps.evaluation.funchisq.FunChisqGraphs.FunChisqGraph
import tps.evaluation.funchisq.FunChisqGraphs.FunChisqScore

object FunChisqParser {
  def run(f: File): FunChisqGraph = {
    val data = new TSVSource(f, noHeaders = false).data
    assert(data.fields == Seq("source", "target", "statistic", "p-value"))

    val pairs = data.tuples map {
      case Seq(p1, p2, statString, pValString) => {
        val edge = GraphParsing.lexicographicEdge(p1, p2)
        val edgeDir = GraphParsing.lexicographicForwardDirection(p1, p2)
        val statistic = statString.toDouble
        val pValue = pValString.toDouble
        (edge, (edgeDir, FunChisqScore(statistic, pValue)))
      }
    }

    pairs.toMap
  }
}
