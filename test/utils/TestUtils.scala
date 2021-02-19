package utils

import java.io.{File, FileInputStream, StringWriter}
import java.nio.charset.Charset

import org.apache.commons.io.IOUtils

object TestUtils {
  def readFile(path: String): String = {
    val stringWriter: StringWriter = new StringWriter()
    IOUtils.copy(new FileInputStream(new File(path)), stringWriter, Charset.defaultCharset().name())
    stringWriter.toString
  }
}
