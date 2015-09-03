package tps.util

object LogUtils {
  import scala.language.postfixOps

  private val escapeStr = (27 toChar).toString
  private val resetStr = escapeStr + "[0m"

  private val red     = "31"
  private val green   = "32"
  private val yellow  = "33"

  private def colorize(message: String, color: String): String = {
    escapeStr + "[" + color + "m" + message + resetStr
  }

  def log(a: Any): Unit = {
    println(a.toString)
  }

  def logWarning(a: Any): Unit = {
    val warningString = colorize("[WARNING]", yellow) + " " + a.toString
    println(warningString)
  }

  def logError(a: Any): Unit = {
    val errorString = colorize("[ERROR]", red) + " " + a.toString
    println(errorString)
  }

  def logSuccess(a: Any): Unit = {
    val successString = colorize("[SUCCESS]", green) + " " + a.toString
    println(successString)
  }

  def terminate(msg: String): Nothing = {
    logError(msg)
    sys.exit(1)
  }

}

