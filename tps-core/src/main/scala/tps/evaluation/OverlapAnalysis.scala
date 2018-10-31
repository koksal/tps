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
      val protsChangingAtStep = significantTimeSeries.profiles.flatMap{ p =>
        proteinsWithSignificantChange(p, i, interp, mapping)
      }.toSet

      val refProtsChangingAtStep = protsChangingAtStep.intersect(refProtNames)
      val ratioOfRefOverlap = refProtsChangingAtStep.size.toDouble /
        protsChangingAtStep.size
      val row = List(
        refName,
        label,
        protsChangingAtStep.size,
        refProtsChangingAtStep.size,
        ratioOfRefOverlap
      )
      println(row.mkString("\t"))
    }

  }

  def proteinsWithSignificantChange(
    profile: Profile,
    step: Int,
    interpretation: TriggerInterpretation,
    peptideToProteins: Map[String, Set[String]]
  ): Set[String] = {
    val profileProteins = peptideToProteins(profile.id)
    if (interpretation.allowedActivationIntervals(profile).contains(step) ||
      interpretation.allowedInhibitionIntervals(profile).contains(step)) {
      profileProteins
    } else {
      Set[String]()
    }
  }

}
