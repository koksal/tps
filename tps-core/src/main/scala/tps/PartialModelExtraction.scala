package tps

import Graphs._

object PartialModelExtraction {
  def run(f: java.io.File): SignedDirectedGraph = {
    val data = new TSVSource(f, noHeaders = true).data
    val pairs = data.tuples map { tuple =>
      val Seq(id1, tpe, id2) = tuple
      tpe match {
        case "N" => lexicographicEdge(id1, id2) -> Set(lexicographicActivation(id1, id2), lexicographicInhibition(id1, id2))
        case "A" => lexicographicEdge(id1, id2) -> Set(lexicographicActivation(id1, id2))
        case "I" => lexicographicEdge(id1, id2) -> Set(lexicographicInhibition(id1, id2))
      }
    }

    var sol = Map[Edge, Set[SignedDirectedEdgeLabel]]()
    for ((e, ess) <- pairs) {
      sol.get(e) match {
        case None => sol += e -> ess
        case Some(existing) => sol += e -> (existing ++ ess)
      }
    }
    sol
  }

  private def lexicographicActivation(src: String, dst: String): SignedDirectedEdgeLabel = {
    val dir = if (src < dst) Forward else Backward
    ActiveEdge(dir, Activating)
  }

  private def lexicographicInhibition(src: String, dst: String): SignedDirectedEdgeLabel = {
    val dir = if (src < dst) Forward else Backward
    ActiveEdge(dir, Inhibiting)
  }

  private def lexicographicEdge(id1: String, id2: String): Edge = {
    if (id1 < id2) Edge(Vertex(id1), Vertex(id2)) else Edge(Vertex(id2), Vertex(id1))
  }
}
