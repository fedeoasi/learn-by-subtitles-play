package serialization

import java.text.SimpleDateFormat

import org.json4s.DefaultFormats

object JsonFormats extends DefaultFormats {
  override protected def dateFormatter: SimpleDateFormat = {
    val f = new SimpleDateFormat("HH:mm:ss.SSS")
    f.setTimeZone(DefaultFormats.UTC)
    f
  }
}
