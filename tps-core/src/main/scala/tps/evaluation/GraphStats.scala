package tps.evaluation

import java.io.File

import tps.Graphs.UndirectedGraph
import tps.synthesis.Synthesis
import tps.{TimeSeries, UndirectedGraphParser}
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

  /**
    *
    * @param g
    * @param ts
    */
  def computeDataCoverageStats(g: UndirectedGraph, ts: TimeSeries): Unit = {
    val profileIds = ts.profiles.map(_.id).toSet
    val verticesWithData = g.V.map(_.id).intersect(profileIds)
    val coverageRatio = verticesWithData.size.toDouble / g.V.size
    println(s"Ratio of vertices with data: ${coverageRatio}")
  }

  def computeProfileStats(
    ts: TimeSeries,
    firstScores: Map[String, Seq[Double]],
    prevScores: Map[String, Seq[Double]],
    threshold: Double
  ): Unit
  = {
    val nbSigMeasurements = ts.profiles map { p =>
      Synthesis.nbSignificantMeasurements(p, firstScores, prevScores, threshold)
    }
    val med = MathUtils.median(nbSigMeasurements.map(_.toDouble))
    println(s"Median number of significant time points: $med")
  }

  def main(args: Array[String]): Unit = {
    val graphName = args(0)
    val graph = UndirectedGraphParser.run(new File(graphName))
    computeGraphStats(graph)
  }
}
