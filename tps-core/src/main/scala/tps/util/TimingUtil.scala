package tps.util

/**
  * Provides utility functions to measure running time.
  */
object TimingUtil {
  def time[R](label: String)(block: => R): R = {
    val t0 = System.nanoTime()
    val result = block    // call-by-name
    val t1 = System.nanoTime()
    val s = (t1 - t0) / 1000000000.0
    println(s"[${label}] Elapsed time: ${s}s")
    result
  }
}
