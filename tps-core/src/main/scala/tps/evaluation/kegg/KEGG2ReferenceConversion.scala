package tps.evaluation.kegg

import java.io.File

import tps.SignedDirectedNetworkPrinter
import tps.util.FileUtils

object KEGG2ReferenceConversion {

  def main(args: Array[String]): Unit = {
    assert(args.size == 2)

    val inputFile = new File(args(0))
    val outputFile = new File(args(1))

    val sifNetwork = KEGGParser.parseMostSpecificLabels(inputFile)

    FileUtils.writeToFile(
      outputFile,
      SignedDirectedNetworkPrinter.print(sifNetwork)
    )
  }

}
