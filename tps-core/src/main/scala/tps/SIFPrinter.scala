package tps

import Graphs._
import SignedDirectedGraphOps._

object SIFPrinter {
  def print(sdg: SignedDirectedGraph): String = {
    val sb = new StringBuffer()

    for ((e, ess) <- sdg) {
      if (ambiguousDirection(ess)) {
        // convert ambiguous directions into undirected edges
        sb append List(e.v1.id, "U", e.v2.id).mkString("\t")
        sb append "\n"
      } else {
        val (src, tgt) = if (canBeForward(ess)) {
          (e.v1.id, e.v2.id)
        } else {
          assert(canBeBackward(ess))
          (e.v2.id, e.v1.id)
        }
        val rel = if (ambiguousSign(ess)) {
          "N"
        } else if (canBeActivating(ess)) {
          "A"
        } else if (canBeInhibiting(ess)) {
          "I"
        } else {
          assert(false)
        }
        sb append List(src, rel, tgt).mkString("\t")
        sb append "\n"
      }
    }
    sb.toString
  }
}
