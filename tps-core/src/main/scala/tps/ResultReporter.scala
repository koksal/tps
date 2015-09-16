package tps

import util.FileUtils
import util.LogUtils

import java.io.File

trait ResultReporter {
  def output(name: String, content: String): Unit
}

class FileReporter(
  outFolder: File, outLabel: Option[String]
) extends ResultReporter {
  def output(name: String, content: String): Unit = {
    val fname = outLabel match {
      case Some(prefix) => s"$prefix-$name"
      case None => name
    }
    val f = new File(outFolder, fname)
    FileUtils.writeToFile(f, content)
    LogUtils.log(s"Wrote file: ${f.getAbsolutePath}")
  }
}

class ConsoleReporter extends ResultReporter {
  def output(name: String, content: String): Unit = {
    LogUtils.log(s"Output: $name")
    LogUtils.log(content)
  }
}

class NoopReporter extends ResultReporter {
  def output(name: String, content: String): Unit = { }
}
