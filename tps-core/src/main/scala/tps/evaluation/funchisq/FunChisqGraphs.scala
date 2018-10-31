package tps.evaluation.funchisq

import tps.Graphs.Edge
import tps.Graphs.Vertex

object FunChisqGraphs {

  // No lexicographic order imposed on edges
  type FunChisqGraph = Seq[(Edge, FunChisqScore)]
  case class FunChisqScore(statistic: Double, pValue: Double)

  def mapNodes(
    g: FunChisqGraph, mapping: Map[String, Set[String]]
  ): FunChisqGraph = {
    // flatmap to all protein-level edges
    val mappedPairSeq = g flatMap {
      case (edge, score) => {
        for {
          mappedV1 <- mapping(edge.v1.id)
          mappedV2 <- mapping(edge.v2.id)
        } yield {
          val mappedEdge = Edge(
            Vertex(mappedV1),
            Vertex(mappedV2)
          )
          (mappedEdge, score)
        }
      }
    }

    // consolidate duplicate edges (take edge with minimum p-value)
    val groupedPairs = mappedPairSeq.groupBy(_._1)
    groupedPairs.toSeq map {
      case (edge, pairSet) => {
        pairSet.minBy(_._2.pValue)
      }
    }
  }
}
