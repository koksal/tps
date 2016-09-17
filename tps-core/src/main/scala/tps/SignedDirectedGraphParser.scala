package tps

import Graphs._
import GraphParsing._

object SignedDirectedGraphParser {
  def run(f: java.io.File): SignedDirectedGraph = {
    val data = new TSVSource(f, noHeaders = true).data
    val pairs = data.tuples map { tuple =>
      val Seq(id1, tpe, id2) = tuple
      val edge = lexicographicEdge(id1, id2)
      tpe match {
        case "N" => edge -> Set(
          lexicographicActivation(id1, id2), 
          lexicographicInhibition(id1, id2)
        )
        case "A" => edge -> Set(lexicographicActivation(id1, id2))
        case "I" => edge -> Set(lexicographicInhibition(id1, id2))
        case "U" => edge -> Set(
          lexicographicActivation(id1, id2), 
          lexicographicInhibition(id1, id2),
          lexicographicActivation(id2, id1), 
          lexicographicInhibition(id2, id1)
        )
      }
    }
    aggregateLabels(pairs)
  }
}
