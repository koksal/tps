package tps.evaluation

import java.io.File

import tps.Graphs.UndirectedGraph
import tps.UndirectedGraphParser

object UndirectedNetworkComparison {
  def main(args: Array[String]): Unit = {
    val n1File = new File(args(0))
    val n2File = new File(args(1))

    val network1 = UndirectedGraphParser.run(n1File)
    val network2 = UndirectedGraphParser.run(n2File)

    println(s"Network 1: ${n1File.getPath}")
    println(s"Network 2: ${n2File.getPath}")

    undirectedNetworkComparison(network1, network2)
  }

  def undirectedNetworkComparison(
    network1: UndirectedGraph,
    network2: UndirectedGraph
  ): Unit = {
    val commonV = network1.V intersect network2.V
    val commonE = network1.E intersect network2.E

    val exclusiveV1 = network1.V -- network2.V
    val exclusiveV2 = network2.V -- network1.V

    val exclusiveE1 = network1.E -- network2.E
    val exclusiveE2 = network2.E -- network1.E

    println(s"Common V: ${commonV.size}")
    println(s"Common E: ${commonE.size}")

    println(s"Only in V1: ${exclusiveV1.size}")
    println(exclusiveV1.mkString("\n"))
    println(s"Only in V2: ${exclusiveV2.size}")
    println(exclusiveV2.mkString("\n"))

    println(s"Only in E1: ${exclusiveE1.size}")
    println(exclusiveE1.mkString("\n"))
    println(s"Only in E2: ${exclusiveE2.size}")
    println(exclusiveE2.mkString("\n"))
  }
}
