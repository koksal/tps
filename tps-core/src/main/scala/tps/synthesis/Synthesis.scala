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
    def filterWithTimeSeries(ppm: Map[String, Set[String]], ts: TimeSeries) = {
      ppm filter { case (peptID, _) =>
        ts.profiles.exists(_.id == peptID)
      }
    }

    val sourceVerticesInNetwork = sources.map(Vertex(_)).intersect(network.V)
    assert(sourceVerticesInNetwork.nonEmpty,
      "No source nodes appear in input network.")
    val networkWithSources = network.copy(sources = sourceVerticesInNetwork)

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

    printCollapsedInterpretation()

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
      case _ =>
        throw new Exception("Only the dataflow solver is supported.")
    }

    val expandedSol = solver.summary()
    collapseSolution(expandedSol, ppmWithData)
  }

  def profileIsSignificant(
    p: Profile, 
    firstScores: Map[String, Seq[Double]], 
    prevScores: Map[String, Seq[Double]],
    threshold: Double
  ): Boolean = {
    val fs = firstScores(p.id)
    val ps = prevScores(p.id)
    val toEval = p.values.tail
    val filteredValues = for ((v, (f, p)) <- toEval zip (fs zip ps)) yield {
      if (f < threshold || p < threshold) v else None
    }
    filteredValues.exists(_.isDefined)
  }

}
