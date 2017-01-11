package tps.evaluation.funchisq

import java.io.File

import tps.PeptideProteinMappingParser
import tps.evaluation.parsing.FunChisqParser

object FunChisq2SIFConverter {

  def main(args: Array[String]): Unit = {
    // parse arguments
    val networkFile = new File(args(0))
    val mappingFile = new File(args(1))
    val pValueThreshold = args(2).toDouble
    val maxNbEdges = args(3).toInt

    // read FC output
    var funChisqGraph = FunChisqParser.run(networkFile)

    // read mapping
    val mapping = PeptideProteinMappingParser.run(mappingFile)

    // map nodes
    funChisqGraph = FunChisqGraphs.mapNodes(funChisqGraph, mapping)

    // filter by given p-value threshold
    funChisqGraph = funChisqGraph filter {
      case (_, score) => score.pValue <= pValueThreshold
    }

    // order by decreasing statistic and limit to first N edges
    val strongestEdges = funChisqGraph.sortBy{
      case (_, score) => score.statistic
    }.reverse.take(maxNbEdges)

    // print in SIF format
    println(FunChisqSIFPrinter.print(strongestEdges))
  }

}
