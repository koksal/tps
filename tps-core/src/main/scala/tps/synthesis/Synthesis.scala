package tps.synthesis

import tps.Graphs._
import tps.SignedDirectedGraphOps._
import tps._

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

    val expandedSol = solver.summary()
    collapseSolution(expandedSol, peptideProteinMap)
  }
}
