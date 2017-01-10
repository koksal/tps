package tps.evaluation.funchisq

import java.io.File

import tps.evaluation.parsing.FunChisqParser

object FunChisq2SIFConverter {

  def main(args: Array[String]): Unit = {
    // parse arguments
    val f = new File(args(0))
    val pValueThreshold = args(1).toDouble
    val maxNbEdges = args(2).toInt

    // read FC output
    var funChisqGraph = FunChisqParser.run(f)

    // filter by given p-value threshold
    funChisqGraph = funChisqGraph filter {
      case (_, (_, score)) => score.pValue <= pValueThreshold
    }

    // order by decreasing statistic and limit to first N edges
    val strongestEdges = funChisqGraph.toSeq.sortBy{
      case (_, (_, score)) => score.statistic
    }.reverse.take(maxNbEdges)

    // print in SIF format
    println(FunChisqSIFPrinter.print(strongestEdges.toMap))
  }

}
