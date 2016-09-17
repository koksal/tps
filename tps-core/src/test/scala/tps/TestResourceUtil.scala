package tps

import java.io.File

object TestResourceUtil {
  private val RESOURCE_BASE_FOLDER = "/simple-example"

  def testFile(name: String): File = {
    val url = getClass.getResource(s"$RESOURCE_BASE_FOLDER/$name")
    new File(url.getFile())
  }
}
