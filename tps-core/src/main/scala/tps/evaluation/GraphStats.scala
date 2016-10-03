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

  def computeDataCoverageStats(g: UndirectedGraph, ts: TimeSeries): Unit = {
    val profileIds = ts.profiles.map(_.id).toSet
    val verticesWithData = g.V.map(_.id).intersect(profileIds)
    val coverageRatio = verticesWithData.size.toDouble / g.V.size
    println(s"Ratio of vertices with data: ${coverageRatio}")
  }

  /**
    * Prints the mean number of phosphosites per protein in the given graph.
    *
    * The graph and time series data are not expanded.
    */
  def printMeanNbPhosphosites(
    g: UndirectedGraph, ts: TimeSeries, ppm: Map[String, Set[String]]
  ): Unit = {
    // TODO remove this for the case where there is no mapping.
    assert(g.V.map(_.id).intersect(ts.profiles.map(_.id).toSet).isEmpty)
    val phosphositeCardinalities = g.V.toSeq map { v =>
      // get peptides that map to the protein
      val matchingPeptides = ppm.filter{ case (pep, prots) =>
        prots contains v .id }.keySet
      val matchingProfiles = ts.profiles.filter{ p =>
        matchingPeptides contains p.id
      }
      matchingProfiles.size
    }

    val mean = MathUtils.mean(phosphositeCardinalities.map(_.toDouble))
    println(s"Mean number of phosphosites: $mean")
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
    val mean = MathUtils.mean(nbSigMeasurements.map(_.toDouble))
    println(s"Mean number of significant time points: $mean")
  }

  def main(args: Array[String]): Unit = {
    val graphName = args(0)
    val graph = UndirectedGraphParser.run(new File(graphName))
    computeGraphStats(graph)
  }
}
