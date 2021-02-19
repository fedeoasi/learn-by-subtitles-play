package subtitles

import org.apache.ws.commons.util.Base64
import java.util.zip.GZIPInputStream
import java.io.{ BufferedOutputStream, OutputStream, ByteArrayOutputStream, ByteArrayInputStream }
import org.apache.commons.io.IOUtils

object OpenSubtitlesDecoder {
  private def base64decode(encoded: String): Array[Byte] =
    Base64.decode(encoded)

  private def gunzip(compressed: Array[Byte]): Array[Byte] = {
    val gis: GZIPInputStream = new GZIPInputStream(
      new ByteArrayInputStream(compressed)
    )
    val baos: ByteArrayOutputStream = new ByteArrayOutputStream
    val out: OutputStream = new BufferedOutputStream(baos)
    IOUtils.copy(gis, out)
    out.flush()
    out.close()
    baos.toByteArray
  }

  def decode(data: String): String = {
    val decodedBytes = base64decode(data)
    new String(gunzip(decodedBytes))
  }
}
