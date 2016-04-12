package utils

import java.io._
import org.apache.commons.io.IOUtils

object Utils {
  def readFileIntoString(path: String): String = {
    val stringWriter: StringWriter = new StringWriter()
    IOUtils.copy(new FileInputStream(new File(path)), stringWriter)
    stringWriter.toString
  }

  def writeStringIntoFile(string: String, path: String) {
    val outputStream: FileOutputStream = new FileOutputStream(path)
    IOUtils.copy(new StringReader(string), outputStream)
    outputStream.close()
  }
}
