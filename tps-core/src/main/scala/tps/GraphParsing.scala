package tps

import Graphs._

object GraphParsing {
  def lexicographicEdge(id1: String, id2: String): Edge = {
    if (id1 < id2) Edge(Vertex(id1), Vertex(id2)) else Edge(Vertex(id2), Vertex(id1))
  }

  def lexicographicDirection(src: String, dst: String, d: EdgeDirection): EdgeDirection = {
    if (src < dst) d else SignedDirectedGraphOps.reverse(d)
  }

  def lexicographicLabel(
    src: String, 
    dst: String, 
    l: SignedDirectedEdgeLabel
  ): SignedDirectedEdgeLabel = {
    if (src < dst) l else SignedDirectedGraphOps.reverse(l)
  }

  def lexicographicForwardDirection(src: String, dst: String): EdgeDirection = {
    if (src < dst) Forward else Backward
  }

  def lexicographicActivation(src: String, dst: String): SignedDirectedEdgeLabel = {
    ActiveEdge(lexicographicForwardDirection(src, dst), Activating)
  }

  def lexicographicInhibition(src: String, dst: String): SignedDirectedEdgeLabel = {
    ActiveEdge(lexicographicForwardDirection(src, dst), Inhibiting)
  }

  def aggregateLabels[T](
    allLabels: Seq[(Edge, Set[T])]
  ): Map[Edge, Set[T]] = {
    var sol = Map[Edge, Set[T]]()
    for ((e, ess) <- allLabels) {
      sol.get(e) match {
        case None => sol += e -> ess
        case Some(existing) => sol += e -> (existing ++ ess)
      }
    }
    sol
  }
}
