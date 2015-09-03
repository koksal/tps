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

    println("Running solver.")
    val outputNetwork = synthesis.Synthesis.run(
      network,
      timeSeries,
      firstScores,
      prevScores,
      partialModels,
      peptideProteinMap,
      opts.sources,
      opts.significanceThreshold,
      opts.synthesisOptions
    )

    // TODO
    // write output network in more detailed format
    val outSIFFilename = opts.outLabel match {
      case Some(prefix) => prefix + "_output.sif"
      case None => "output.sif"
    }
    val outNetworkFile = new java.io.File(opts.outFolder, outSIFFilename)
    util.FileUtils.writeToFile(outNetworkFile, SIFPrinter.print(outputNetwork))
    println(s"Output sif file written to: ${outSIFFilename}")
  }
}
