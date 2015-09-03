package tps.util

import java.io._

object FileUtils {
  def writeToFile(f: File, content: String, append: Boolean = false) {

    // create parent if nonexistent
    val parent = f.getParentFile
    if (parent != null) parent.mkdirs

    val fw = new FileWriter(f.getAbsolutePath, append)
    val bw = new BufferedWriter(fw)
    val out = new PrintWriter(bw)
    try { 
      out.print(content) 
    } finally { 
      out.close 
    }
  }

  def lines(f: File): List[String] = lines(f.getAbsolutePath)
  def lines(fname: String): List[String] = {

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

  def fileExists(fn: String): Boolean = new File(fn).isFile

  def sanitizeFilename(fn: String): String = fn.replaceAll("/", "_")
}
