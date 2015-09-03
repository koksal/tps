package tps.util

import java.io.File
import scala.sys.process._

object FileUtils {
  def writeToFile(f: File, content: String) {
    writeToFile(f.getAbsolutePath, content)
  }

  def writeToFile(fname: String, content: String, append: Boolean = false) {
    import java.io._
    import scala.io._

    // create parent if nonexistent
    val f = new File(fname)
    val parent = f.getParentFile
    parent.mkdirs

    val out = new PrintWriter(new BufferedWriter(new FileWriter(fname, append)))
    try{ out.print( content ) }
    finally{ out.close }
  }

  def linesOld(fname: String): List[String] = {
    val src = scala.io.Source.fromFile(fname)
    val lines = src.getLines.toList
    src.close()
    lines
  }

  def lines(f: File): List[String] = lines(f.getAbsolutePath)
  def lines(fname: String): List[String] = {
    import java.io.{BufferedReader,FileReader}

    val br = new BufferedReader(new FileReader(fname))
    var lines: List[String] = Nil
    try {
      var line = br.readLine();

      while (line != null) {
        lines = line :: lines
        line = br.readLine()
      }

      lines.reverse
    } finally {
      br.close()
    }
  }

  def uncommentedLines(f: File): List[String] = uncommentedLines(f.getAbsolutePath)
  def uncommentedLines(fname: String): List[String] = {
    lines(fname) filter (!_.startsWith("#"))
  }

  def testAndMakeFolder(fname: String): Unit = {
    ("mkdir -p " + fname).!!
  }

  def makeLink(fname: String, lname: String): Unit = {
    (s"rm -rf $lname").!!
    ("ln -sf " + fname + " " + lname).!!
    LogUtils.log("Link " + lname + " created.")
  }

  def fileExists(fn: String): Boolean = new java.io.File(fn).isFile

  def sanitizeFilename(fn: String): String = fn.replaceAll("/", "_")
}
