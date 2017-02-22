package tps

import Graphs._
import GraphParsing._

object ReferenceParser {
  def run(f: java.io.File): (SignedDirectedGraph, Map[Edge, String]) = {
    val data = new TSVSource(f, noHeaders = false).data


    var evidencePerEdge: Map[Edge, String] = Map.empty

    val tuples = data.tuples.map{ tuple =>
      assert(tuple.size >= 6)
      
      val Seq(src, tgt, lra, lri, rla, rli, rest @ _*) = tuple
      val edge = lexicographicEdge(src, tgt)

      assert(!evidencePerEdge.isDefinedAt(edge))
      evidencePerEdge += edge -> rest.mkString(", ")
      var originalLabels = Set[SignedDirectedEdgeLabel]()

      def labelValue(l: String): Boolean = l match {
        case "true" => true
        case "false" => false
      }

      if (labelValue(lra)) originalLabels += ActiveEdge(Forward, Activating)
      if (labelValue(lri)) originalLabels += ActiveEdge(Forward, Inhibiting)
      if (labelValue(rla)) originalLabels += ActiveEdge(Backward, Activating)
      if (labelValue(rli)) originalLabels += ActiveEdge(Backward, Inhibiting)

      val lexicOrientedLabels = originalLabels map { l => lexicographicLabel(src, tgt, l) }

      (edge, lexicOrientedLabels)
    }

    (aggregateLabels(tuples), evidencePerEdge)
  }
}
