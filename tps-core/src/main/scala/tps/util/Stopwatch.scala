package tps.util

/** Implements a stopwatch for profiling purposes */
class Stopwatch(description : String, verbose : Boolean = false) {
  var beginning: Long = 0L
  var end: Long = 0L
  var acc: Long = 0L

  def start : Stopwatch = {
    beginning = System.currentTimeMillis
    this
  }

  def stop : Double = {
    end = System.currentTimeMillis
    acc += (end - beginning)
    val seconds = (end - beginning) / 1000.0
    if (verbose) println("Checkpoint %-25s: %-3.2fs" format (description, seconds))
    seconds
  }

  def elapsed : Double = {
    val now = System.currentTimeMillis
    val elapsedMillis = now - beginning
    val elapsedSeconds = elapsedMillis / 1000.0
    elapsedSeconds
  }
}
