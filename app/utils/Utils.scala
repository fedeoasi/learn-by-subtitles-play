package utils

import java.io._
import java.nio.charset.Charset

import org.apache.commons.io.IOUtils

object Utils {
  private val charset = Charset.defaultCharset()

  def readFileIntoString(path: String): String = {
    val stringWriter: StringWriter = new StringWriter()
    IOUtils.copy(
      new FileInputStream(new File(path)),
      stringWriter,
      charset.name()
    )
    stringWriter.toString
  }

  def writeStringIntoFile(string: String, path: String) {
    val outputStream: FileOutputStream = new FileOutputStream(path)
    IOUtils.copy(new StringReader(string), outputStream, charset.name())
    outputStream.close()
  }
}
