package tps

object Main {
  def main(args: Array[String]): Unit = {
    val opts = ArgHandling.parseOptions(args)

    val network       = NetworkExtraction.run(opts.networkPath)
    val timeSeries    = TimeSeriesExtraction.run(opts.timeSeriesPath)
    val firstScores   = TimeSeriesScoresExtraction.run(opts.firstScoresPath)
    val prevScores    = TimeSeriesScoresExtraction.run(opts.prevScoresPath)
    val partialModels = opts.partialModelPaths map { p =>
      PartialModelExtraction.run(p)
    }
    val peptideProteinMap = opts.peptideProteinMapPath map { p =>
      PeptideProteinMappingExtraction.run(p)
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

    // TODO
    // write output network in more detailed format

    resultReporter.output("output.sif", SIFPrinter.print(outputNetwork))
  }
}
