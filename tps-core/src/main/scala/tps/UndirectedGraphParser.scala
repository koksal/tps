package tps

import Graphs._

/** Extracts edge node pairs froma a file without a header row. */
object UndirectedGraphParser {
  def run(f: java.io.File): UndirectedGraph = {
    val data = new TSVSource(f, noHeaders = true).data
    val edges = data.tuples map { tuple =>
      assert(tuple.size == 2)
      val id1 = tuple(0)
      val id2 = tuple(1)
      if (id1 < id2) {
        Edge(Vertex(id1), Vertex(id2))
      } else {
        Edge(Vertex(id2), Vertex(id1))
      }
    }
    UndirectedGraph(edges)
  }
}
