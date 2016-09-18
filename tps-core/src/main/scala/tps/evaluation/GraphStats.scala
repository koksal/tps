package tps.evaluation

import java.io.File

import tps.Graphs.UndirectedGraph
import tps.UndirectedGraphParser
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

  def main(args: Array[String]): Unit = {
    val graphName = args(0)
    val graph = UndirectedGraphParser.run(new File(graphName))
    computeGraphStats(graph)
  }
}
