package tps

import Graphs._

object UndirectedGraphOps {
  def union(g1: UndirectedGraph, g2: UndirectedGraph): UndirectedGraph = {
    val uV = g1.V ++ g2.V
    val uE = g1.E ++ g2.E
    val uSrc = g1.sources ++ g2.sources
    UndirectedGraph(uV, uE, uSrc)
  }

  def cliqueEdges(vs: Set[Vertex]): Set[Edge] = {
    if (vs.isEmpty)  {
      Set[Edge]()
    } else {
      val e = vs.head
      val rest = vs - e
      val eEdges = rest.map(Edge(e,_))
      eEdges ++ cliqueEdges(rest)
    }
  }

  def clique(vs: Set[Vertex]): UndirectedGraph = {
    UndirectedGraph(vs, cliqueEdges(vs), Set.empty)
  }

  def emptyGraph: UndirectedGraph = UndirectedGraph(Set.empty, Set.empty, Set.empty)

  def fromDirectedGraph(g: DirectedGraph): UndirectedGraph = {
    var V: Set[Vertex] = Set.empty
    val E = g.keySet
    for (e <- E) {
      V ++= Set(e.v1, e.v2)
    }
    UndirectedGraph(V, E, Set.empty)
  }
}
