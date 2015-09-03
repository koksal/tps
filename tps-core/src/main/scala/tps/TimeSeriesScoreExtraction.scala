package tps

object TimeSeriesScoresExtraction {
  def run(f: java.io.File): Map[String, Seq[Double]] = {
    val data = new TSVSource(f, noHeaders = true).data
    val m = data.tuples map { tuple => 
      val id = tuple.head
      val values = tuple.tail map (_.toDouble)
      id -> values
    }
    m.toMap
  }
}
