package tps

class TSVSource(f: java.io.File, noHeaders: Boolean = false) {
  private lazy val ls = util.FileUtils.uncommentedLines(f)

  def data: TabularData = {
    val fields = {
      if (noHeaders) Nil
      else ls.head.split("\t").toList
    }
    val tuples = {
      val tplLines = if (noHeaders) ls else ls.tail
      tplLines.map(_.split("\t", -1).toList)
    }
   TabularData(fields, tuples) 
  }
}
