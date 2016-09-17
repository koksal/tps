package tps

import Graphs._

case class AdjacencyMatrix(labels: IndexedSeq[String], matrix: Array[Array[Boolean]]) {
  def toTabularData: TabularData = {
    val tuples = matrix.map{ row =>
      row.map{ entry => if (entry) "1" else "0" }.toSeq
    }.toSeq
    TabularData(Seq(), tuples)
  }
}

object AdjacencyMatrix {
  def create(
    ts: TimeSeries,
    pin: DirectedGraph,
    peptideProteinMap: Map[String, Set[String]]
  ): AdjacencyMatrix = {
    val labels = ts.profiles.map(_.id).toIndexedSeq
    val matrix = Array.ofDim[Boolean](labels.size, labels.size)

    for {
      (pep1, i) <- labels.zipWithIndex
      (pep2, j) <- labels.zipWithIndex
    } {
      if (i == j) {
        // we mark self edges
        matrix(i)(j) = true
      }
      val prots1 = peptideProteinMap.getOrElse(pep1, Set[String]())
      val prots2 = peptideProteinMap.getOrElse(pep2, Set[String]())

      for {
        prot1 <- prots1
        prot2 <- prots2
      } {
        val ds = getDirections(pin, prot1, prot2)
        if (ds contains Forward) matrix(i)(j) = true
        if (ds contains Backward) matrix(j)(i) = true
      }
    }
    AdjacencyMatrix(labels, matrix)
  }
}
