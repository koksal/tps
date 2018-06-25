package tps.evaluation.kegg

import java.io.File

import tps.GraphParsing
import tps.Graphs.SignedDirectedGraph
import tps.TSVSource

object KEGGParser {

  def parseMostSpecificLabels(f: File): SignedDirectedGraph = {
    val data = new TSVSource(f, noHeaders = false).data

    val pairs = data.tuples map { tuple =>
      val Seq(id1, tpe, id2) = tuple
      val edge = GraphParsing.lexicographicEdge(id1, id2)

      tpe match {
        case "activation" => edge -> Set(
          GraphParsing.lexicographicActivation(id1, id2)
        )
        case "inhibition" => edge -> Set(
          GraphParsing.lexicographicInhibition(id1, id2)
        )
        case "binding/association" | "expression" => edge -> Set(
          GraphParsing.lexicographicActivation(id1, id2),
          GraphParsing.lexicographicInhibition(id1, id2),
          GraphParsing.lexicographicActivation(id2, id1),
          GraphParsing.lexicographicInhibition(id2, id1)
        )
      }
    }

    GraphParsing.aggregateBySmallestLabelSets(pairs)
  }

}
