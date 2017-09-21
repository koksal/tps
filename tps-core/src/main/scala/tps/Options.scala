package tps

import tps.synthesis.SynthesisOptions

object Options {
  val VERSION = "2.1"
}

case class Options(
  synthesisOptions:               SynthesisOptions = SynthesisOptions(),
  networkPath:                    java.io.File = null,
  timeSeriesPath:                 java.io.File = null,
  firstScoresPath:                java.io.File = null,
  prevScoresPath:                 java.io.File = null,
  partialModelPaths:              Set[java.io.File] = Set(),
  peptideProteinMapPath:          Option[java.io.File] = None,
  outLabel:                       Option[String] = None,
  outFolder:                      java.io.File = new java.io.File("."),
  sources:                        Set[String] = Set(),
  significanceThreshold:          Double = 0
)
