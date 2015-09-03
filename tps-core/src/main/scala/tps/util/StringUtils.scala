package tps.util

object StringUtils {
  def indent(s: String): String = {
    s.split("\n").mkString("  ", "\n  ", "")
  }

  def uniquePeptideID(pept: String, prot: String): String = {
    prot + "#" + pept
  }
}
