package tps

import tps.util.LogUtils

case class TabularData(fields: Seq[String], tuples: Seq[Seq[String]]) {
  def toTSVString(printHeaders: Boolean = true): String = {
    val fldLine = fields.mkString("\t")
    val tplLines = tuples.map(_.mkString("\t"))
    val allLines = if (fields.isEmpty || !printHeaders) tplLines else fldLine +: tplLines
    allLines.mkString("\n")
  }

  def project(fs: String*): TabularData = project(fs.toList)

  def project(fs: List[String]): TabularData = {
    val projectedIdx = selIndices(fs)

    val projectedTuples = tuples map { tuple =>
      projectedIdx map (i => tuple(i))
    }
    TabularData(fs, projectedTuples)
  }

  private def selIndices(fs: List[String]): List[Int] = {
    val indices = fs map (fields.indexOf(_))
    
    val notFound = fs.zip(indices) collect { 
      case (field, idx) if idx < 0 => field
    }

    if (notFound.isEmpty)
      indices
    else
      LogUtils.terminate("Projected field(s) not found: " + notFound.mkString(", "))
  }
}
