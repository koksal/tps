package tps.evaluation.funchisq

import tps.Graphs.Edge
import tps.Graphs.EdgeDirection
import tps.Graphs.Vertex

object FunChisqGraphs {

  case class FunChisqScore(statistic: Double, pValue: Double)
  type FunChisqGraph = Map[Edge, (EdgeDirection, FunChisqScore)]

  def mapNodes(
    g: FunChisqGraph, mapping: Map[String, Set[String]]
  ): FunChisqGraph = {
    // flatmap to all protein-level edges
    val mappedPairSeq = g.toSeq flatMap {
      case (edge, (edgeDir, score)) => {
        for {
          mappedV1 <- mapping(edge.v1)
          mappedV2 <- mapping(edge.v2)
        } yield {
          val mappedEdge = Edge(
            Vertex(mappedV1),
            Vertex(mappedV2)
          )
          (mappedEdge, (edgeDir, score))
        }
      }
    }

    // consolidate duplicate edges (take edge with minimum p-value)
    val groupedPairs = mappedPairSeq.groupBy(_._1)
    groupedPairs map {
      case (edge, pairSet) => {
        pairSet.minBy(_._2._2.pValue)
      }
    }
  }
}
