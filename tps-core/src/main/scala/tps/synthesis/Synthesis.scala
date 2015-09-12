package tps.synthesis

import tps.GraphSolutions._
import tps.UndirectedGraphs._
import tps._

object Synthesis {
  def run(
    network: UndirectedGraph,
    timeSeries: TimeSeries,
    firstScores: Map[String, Seq[Double]],
    prevScores: Map[String, Seq[Double]],
    partialModels: Set[AmbiguousGraphSolution],
    peptideProteinMap: Map[String, Set[String]],
    sources: Set[String],
    significanceThreshold: Double,
    opts: SynthesisOptions,
    resultReporter: ResultReporter
  ): AmbiguousGraphSolution = {
    def networkWithSources = network.copy(sources = sources map (Vertex(_)))

    def filterWithTimeSeries(ppm: Map[String, Set[String]], ts: TimeSeries) = {
      ppm filter { case (peptID, _) =>
        ts.profiles.exists(_.id == peptID)
      }
    }

    val unifiedPartialModel = 
      partialModels.foldLeft[AmbiguousGraphSolution](
        Map.empty)(intersectCommonEdges(_,_))

    // filter ts with ppm (?)
    val ppmWithData = filterWithTimeSeries(peptideProteinMap, timeSeries)

    // expand network, ts, scores, pms
    import PeptideExpansion._
    val expandedNetwork       = expandGraph(networkWithSources, peptideProteinMap)
    val expandedTimeSeries    = expandTimeSeries(timeSeries, peptideProteinMap)
    val expandedFirstScores   = expandScores(firstScores, peptideProteinMap)
    val expandedPrevScores    = expandScores(prevScores, peptideProteinMap)
    val expandedPartialModel  = expandSolution(unifiedPartialModel, peptideProteinMap)

    // infer activity intervals from time series data
    val interpretation = new TriggerInterpretation(
      opts,
      expandedNetwork,
      expandedTimeSeries,
      expandedFirstScores,
      expandedPrevScores,
      significanceThreshold
    )

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
          opts,
          interpretation
        )
      case "bilateral" => 
        new BilateralSolver(
          expandedNetwork,
          opts,
          interpretation
        )
    }

    val expandedSol = solver.summary()
    collapseSolution(expandedSol, peptideProteinMap)
  }
}
