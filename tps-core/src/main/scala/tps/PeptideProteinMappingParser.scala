package tps

import tps.util.FileUtils._

object PeptideProteinMappingParser {
  def run(f: java.io.File): Map[String, Set[String]] = {
    val data = new TSVSource(f).data
    val pairs = data.tuples map { tuple =>
      val Seq(peptID, protSeq) = tuple
      val protIDs = protSeq.split("\\|").toSet
      peptID -> protIDs
    }
    pairs.toMap
  }
}
