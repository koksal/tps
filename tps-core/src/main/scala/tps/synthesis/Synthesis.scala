package tps.synthesis

import tps.Graphs._
import tps.SignedDirectedGraphOps._
import tps._
import tps.evaluation.GraphStats
import tps.util.TimingUtil

object Synthesis {
  def run(
    network: UndirectedGraph,
    timeSeries: TimeSeries,
    firstScores: Map[String, Seq[Double]],
    prevScores: Map[String, Seq[Double]],
    partialModels: Set[SignedDirectedGraph],
    peptideProteinMap: Map[String, Set[String]],
    sources: Set[String],
    significanceThreshold: Double,
    opts: SynthesisOptions,
    resultReporter: ResultReporter
  ): SignedDirectedGraph = {
    def networkWithSources = network.copy(sources = sources map (Vertex(_)))

    def filterWithTimeSeries(ppm: Map[String, Set[String]], ts: TimeSeries) = {
      ppm filter { case (peptID, _) =>
        ts.profiles.exists(_.id == peptID)
      }
    }

    val unifiedPartialModel = 
      partialModels.foldLeft[SignedDirectedGraph](
        Map.empty)(unifyByIntersectingCommonEdges(_,_))

    val significantTimeSeries = timeSeries.copy(
      profiles = timeSeries.profiles filter (p => 
          profileIsSignificant(p, firstScores, prevScores, significanceThreshold)
      )
    )

    // filter ts with ppm (do not expand insignificant profiles)
    val ppmWithData = filterWithTimeSeries(peptideProteinMap, significantTimeSeries)

    // expand network, ts, scores, pms
    import PeptideExpansion._
    val expandedNetwork       = expandGraph(networkWithSources, ppmWithData)
    val expandedTimeSeries    = expandTimeSeries(significantTimeSeries, ppmWithData)
    val expandedFirstScores   = expandScores(firstScores, ppmWithData)
    val expandedPrevScores    = expandScores(prevScores, ppmWithData)
    val expandedPartialModel  = expandSolution(unifiedPartialModel, ppmWithData)

    // infer activity intervals from time series data
    val interpretation = new TriggerInterpretation(
      opts.constraintOptions.monotonicity,
      expandedNetwork,
      expandedTimeSeries,
      expandedFirstScores,
      expandedPrevScores,
      significanceThreshold
    )

    def printCollapsedInterpretation() = {
      val collapsedInterpretation = new TriggerInterpretation(
        opts.constraintOptions.monotonicity, 
        networkWithSources, 
        significantTimeSeries, 
        firstScores, 
        prevScores, 
        significanceThreshold
      )
      resultReporter.output(
        "temporal-interpretation.tsv", 
        InterpretationPrinter.print(collapsedInterpretation)
      )
    }

    // debug and stats printing
    printCollapsedInterpretation()
    println("Expanded graph stats:")
    GraphStats.computeGraphStats(expandedNetwork)
    GraphStats.computeDataCoverageStats(expandedNetwork, expandedTimeSeries)
    GraphStats.computeProfileStats(expandedTimeSeries, expandedFirstScores,
      expandedPrevScores, significanceThreshold)

    // dispatch solver
    val solver = opts.solver match {
      case "dataflow" =>
        new DataflowSolver(
          expandedNetwork,
          expandedPartialModel,
          opts,
          interpretation,
          resultReporter
        )
      case "naive" =>
        new NaiveSymbolicSolver(
          expandedNetwork,
          expandedPartialModel,
          opts,
          interpretation
        )
      case "bilateral" =>
        new BilateralSolver(
          expandedNetwork,
          expandedPartialModel,
          opts,
          interpretation
        )
    }

    val expandedSol = TimingUtil.time("solver") {
      solver.summary()
    }
    collapseSolution(expandedSol, ppmWithData)
  }

  def profileIsSignificant(
    p: Profile, 
    firstScores: Map[String, Seq[Double]], 
    prevScores: Map[String, Seq[Double]],
    threshold: Double
  ): Boolean = {
    nbSignificantMeasurements(p, firstScores, prevScores, threshold) > 0
  }

  // TODO move
  def nbSignificantMeasurements(
    p: Profile,
    firstScores: Map[String, Seq[Double]],
    prevScores: Map[String, Seq[Double]],
    threshold: Double
  ): Int = {
    val fs = firstScores(p.id)
    val ps = prevScores(p.id)
    val toEval = p.values.tail
    val filteredValues = for ((v, (f, p)) <- toEval zip (fs zip ps)) yield {
      if (f < threshold || p < threshold) v else None
    }
    filteredValues.filter(_.isDefined).size
  }

}
