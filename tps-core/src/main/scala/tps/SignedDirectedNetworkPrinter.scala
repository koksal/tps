package tps

import GraphSolutions._
import Graphs._

object SignedDirectedNetworkPrinter {
  def print(g: SignedDirectedGraph): String = {
    val sb = new StringBuffer()
    val fields = List(
      "source",
      "target",
      "left-right-act",
      "left-right-inh",
      "right-left-act",
      "right-left-inh"
    )
    sb append fields.mkString("\t")
    sb append "\n"

    for ((Edge(Vertex(v1), Vertex(v2)), ess) <- g) {
      assert(!ess.isEmpty)
      val lra = ess contains ActiveEdge(Forward, Activating)
      val lri = ess contains ActiveEdge(Forward, Inhibiting)
      val rla = ess contains ActiveEdge(Backward, Activating)
      val rli = ess contains ActiveEdge(Backward, Inhibiting)

      val row = List(
        v1,
        v2,
        lra,
        lri,
        rla,
        rli
      )
      sb append row.mkString("\t")
      sb append "\n"
    }

    sb.toString
  }
}
