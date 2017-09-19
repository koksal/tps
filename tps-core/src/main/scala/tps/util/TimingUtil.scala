package tps.util

/**
  * Provides utility functions to measure running time.
  */
object TimingUtil {
  def time[R](label: String)(block: => R): R = {
    val (result, time) = computeAndTime(block _)
    println(s"[${label}] Elapsed time: ${time}s")
    result
  }

  def timeReplicates[R](label: String, replicates: Int)(block: => R): Unit = {
    var times = Set[Double]()
    for (i <- 1 to replicates) {
      val (_, time) = computeAndTime(block _)
      times += time
    }
    val min = times.min
    val max = times.max
    val mean = MathUtils.mean(times)
    val median = MathUtils.median(times)
    println(s"[$label] Elapsed: min = $min, max = $max, mean = $mean, median " +
      s"= $median")
  }

  private def computeAndTime[R](block: () => R): (R, Double) = {
    val t0 = System.nanoTime()
    val result = block()    // call-by-name
    val t1 = System.nanoTime()
    val s = (t1 - t0) / 1000000000.0
    (result, s)
  }
}
