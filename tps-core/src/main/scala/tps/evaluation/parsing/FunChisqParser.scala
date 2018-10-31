package tps.evaluation.parsing

import java.io.File

import tps.Graphs.Edge
import tps.Graphs.Vertex
import tps.TSVSource
import tps.evaluation.funchisq.FunChisqGraphs.FunChisqGraph
import tps.evaluation.funchisq.FunChisqGraphs.FunChisqScore

object FunChisqParser {
  def run(f: File): FunChisqGraph = {
    val data = new TSVSource(f, noHeaders = false).data
    assert(data.fields == Seq("source", "target", "statistic", "p-value"))

    val pairs = data.tuples map {
      case Seq(p1, p2, statString, pValString) => {
        val edge = Edge(Vertex(p1), Vertex(p2))
        val statistic = statString.toDouble
        val pValue = pValString.toDouble
        (edge, FunChisqScore(statistic, pValue))
      }
    }

    pairs
  }
}
