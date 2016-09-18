package tps.evaluation

import tps.Graphs.UndirectedGraph
import tps.util.MathUtils

/**
  * Computes basic graph statistics
  */
object GraphStats {
  def computeGraphStats(g: UndirectedGraph): Unit = {
    // # nodes, # edges, avg degree, median degree
    println(s"# vertices: ${g.V.size}")
    println(s"# edges   : ${g.E.size}")

    val vertexDegrees = g.V.map(v => g.neighbors(v).size.toDouble)
    println(s"Average vertex degree: ${MathUtils.mean(vertexDegrees)}")
    println(s"Median vertex degree : ${MathUtils.median(vertexDegrees)}")

  }
}
