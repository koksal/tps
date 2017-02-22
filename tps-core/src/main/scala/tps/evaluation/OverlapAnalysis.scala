package tps.evaluation

import tps._
import tps.Graphs._
import tps.synthesis._

import java.io.File

object OverlapAnalysis {
  def main(args: Array[String]): Unit = {
    // get reference
    val refFile = new File(args(0))
    val refName = refFile.getName()
    val (refNetwork, refEvidence) = ReferenceParser.run(refFile)

    // get time series
    val tsFile = new File(args(1))
    val timeSeries = TimeSeriesParser.run(tsFile)

    // get mapping
    val mapFile = new File(args(2))
    val mapping = PeptideProteinMappingParser.run(mapFile)

    // first and prev scores
    val firstScores   = TimeSeriesScoresParser.run(new File(args(3)))
    val prevScores    = TimeSeriesScoresParser.run(new File(args(4)))

    val significanceThreshold = 0.01

    // get time series interpretation
    val interp = new TriggerInterpretation(
      useMonotonicity = true,
      graph = UndirectedGraphOps.emptyGraph,
      ts = timeSeries,
      firstScores = firstScores,
      prevScores = prevScores,
      threshold = significanceThreshold
    )

    val significantTimeSeries = timeSeries.copy(
      profiles = timeSeries.profiles filter (p => 
          Synthesis.profileIsSignificant(p, firstScores, prevScores, significanceThreshold)
      )
    )
    println(s"There are ${significantTimeSeries.profiles.size} significant " +
      s"profiles")

    val refProtNames = refNetwork.keySet.flatMap { 
      case Edge(Vertex(id1), Vertex(id2)) => Set(id1, id2)
    }
    println(s"There are ${refProtNames.size} names to check in the network.")

    for ((label, i) <- significantTimeSeries.labels.zipWithIndex) {
      def overlappingProteins(profile: Profile): Set[String] = {
        val prots = mapping(profile.id)
        if ((interp.allowedActivationIntervals(profile) contains i) ||
            (interp.allowedInhibitionIntervals(profile) contains i)) {
          refProtNames intersect prots
        } else {
          Set[String]()
        }
      }
      val overlappingProteinsForStep = significantTimeSeries.profiles.flatMap(overlappingProteins)
      val row = List(refName, label, overlappingProteinsForStep.size)
      println(row.mkString("\t"))
    }

    // total overlap
    val allProtsMappedByProfiles = significantTimeSeries.profiles.flatMap{
      p => mapping(p.id)
    }.toSet
    val totalRow = List(refName, "all",
      (allProtsMappedByProfiles intersect refProtNames).size)
    println(totalRow.mkString("\t"))

  }
}
