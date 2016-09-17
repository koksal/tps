package tps.evaluation.parsing

import tps.Graphs._
import tps.GraphParsing._
import tps.TSVSource

object DBNNetworkParser {
  def run(f: java.io.File, minProb: Double): SignedDirectedGraph = {
    val data = new TSVSource(f, noHeaders = false).data

    val tuplesWithLabels: Seq[(Edge, SignedDirectedEdgeLabel, Double)] = data.tuples.map{ 
      case Seq(p1, p2, prob, sign, pep1, pep2) => {
        val edgeSign = sign match {
          case "-1" => Inhibiting
          case "1"  => Activating
        }
        val edge = lexicographicEdge(p1, p2)
        val edgeDir = lexicographicForwardDirection(p1, p2)
        val label = ActiveEdge(edgeDir, edgeSign)
        (edge, label, prob.toDouble)
      }
    }

    val tuplesAboveThreshold = tuplesWithLabels collect {
      case (edge, label, prob) if prob >= minProb => edge -> Set[SignedDirectedEdgeLabel](label)
    }

    aggregateLabels(tuplesAboveThreshold)
  }
}

