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

  def lines(f: File): List[String] = {
    val br = new BufferedReader(new FileReader(f.getAbsolutePath()))
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

  def uncommentedLines(f: File): List[String] = {
    lines(f) filter (!_.startsWith("#"))
  }

  def fileExists(fn: String): Boolean = new File(fn).isFile

  def sanitizeFilename(fn: String): String = fn.replaceAll("/", "_")
}
