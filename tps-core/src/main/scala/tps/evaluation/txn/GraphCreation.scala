package tps.evaluation.txn

import tps._
import tps.Graphs._

import tps.evaluation.parsing.TXNParser

object GraphCreation {
  def main(args: Array[String]): Unit = {
    val txnFn = args(0)
    val minFlow = args(1).toDouble

    val network = TXNParser.run(txnFn, minFlow)

    val outFile = new java.io.File(s"TXN-flow-$minFlow.sif")
    tps.util.FileUtils.writeToFile(
      outFile,
      SIFPrinter.print(network)
    )
  }
}
