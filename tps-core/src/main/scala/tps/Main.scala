package tps

object Main {
  def main(args: Array[String]): Unit = {
    val opts = ArgHandling.parseOptions(args)

    val network       = UndirectedGraphParser.run(opts.networkPath)
    val timeSeries    = TimeSeriesParser.run(opts.timeSeriesPath)
    val firstScores   = TimeSeriesScoresParser.run(opts.firstScoresPath)
    val prevScores    = TimeSeriesScoresParser.run(opts.prevScoresPath)
    val partialModels = opts.partialModelPaths map { p =>
      SignedDirectedGraphParser.run(p)
    }
    val peptideProteinMap = opts.peptideProteinMapPath map { p =>
      PeptideProteinMappingParser.run(p)
    } getOrElse Map.empty

    val resultReporter = new FileReporter(opts.outFolder, opts.outLabel)

    util.LogUtils.log("Running solver.")
    val outputNetwork = synthesis.Synthesis.run(
      network,
      timeSeries,
      firstScores,
      prevScores,
      partialModels,
      peptideProteinMap,
      opts.sources,
      opts.significanceThreshold,
      opts.synthesisOptions,
      resultReporter
    )

    resultReporter.output("output.sif", SIFPrinter.print(outputNetwork))
    resultReporter.output("output.tsv", SignedDirectedNetworkPrinter.print(outputNetwork))
  }
}
