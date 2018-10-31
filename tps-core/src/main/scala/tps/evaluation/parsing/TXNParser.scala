package tps.evaluation.parsing

import tps.Graphs._
import tps.GraphParsing._
import tps.TSVSource

object TXNParser {
  def run(fn: String, minFlow: Double): SignedDirectedGraph = {
    val f = new java.io.File(fn)
    val data = new TSVSource(f, noHeaders = true).data

    val tuples = data.tuples.map{ 
      case Seq(edgeStr, labelStr, flowStr) => {
        val Seq(id1, id2) = edgeStr.split("~").toSeq
        val flow = flowStr.toDouble
        val edge = lexicographicEdge(id1, id2)
        val edgeDir = lexicographicForwardDirection(id1, id2)
        val labels = Set[SignedDirectedEdgeLabel](
          ActiveEdge(edgeDir, Activating),
          ActiveEdge(edgeDir, Inhibiting)
        )
        (edge, labels, flow)
      }
    }

    val tuplesAboveMinFlow = tuples collect {
      case (edge, labels, flow) if flow >= minFlow => edge -> labels
    }

    aggregateLabels(tuplesAboveMinFlow)
  }
}
