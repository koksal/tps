package tps

import java.io.File

class ResultReporter(outFolder: File, outLabel: Option[String]) {
  def writeOutput(name: String, content: String): Unit = {
    val fname = outLabel match {
      case Some(prefix) => s"$prefix-$name"
      case None => name
    }
    val f = new File(outFolder, fname)
    util.FileUtils.writeToFile(f, content)
    util.LogUtils.log(s"Wrote file: ${f.getAbsolutePath}")
  }
}
