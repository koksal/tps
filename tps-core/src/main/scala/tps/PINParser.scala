package tps

import Graphs._

object PINParser {
  def run(f: java.io.File): DirectedGraph = {
    val data = new TSVSource(f, noHeaders = false).data
    val pairs = data.tuples map { tuple => 
      val Seq(id1, id2, weight, orientation) = tuple
      orientation match {
        case "U" => 
          GraphParsing.lexicographicEdge(id1, id2) -> Set[EdgeDirection](
            Forward, Backward)
        case "D" => 
          GraphParsing.lexicographicEdge(id1, id2) -> Set[EdgeDirection](
            GraphParsing.lexicographicForwardDirection(id1, id2))
      }
    }
    GraphParsing.aggregateLabels(pairs)
  }
}
