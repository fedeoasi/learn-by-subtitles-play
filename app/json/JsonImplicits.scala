package json

import java.text.SimpleDateFormat
import java.util.Date

import model.SubEntry
import org.json4s.JsonAST.{JString, JValue}
import search.{SearchSubtitleResult, DisplayableSubtitleResult}
import org.json4s.JsonDSL._

import scala.language.implicitConversions

object JsonImplicits {
  lazy val df = new SimpleDateFormat("hh:mm:ss,SSS")

  implicit def toJValue(r: DisplayableSubtitleResult): JValue =
    ("title" -> r.movie.title) ~
    ("score" -> r.score) ~
    ("subtitleId" -> r.subtitleId) ~
    ("subEntries" -> r.entries) ~
    ("imdbId" -> r.movie.imdbID)

  implicit def toJValue(r: SearchSubtitleResult): JValue =
    ("title" -> r.title) ~
    ("score" -> r.score) ~
    ("subtitleId" -> r.subtitleId) ~
    ("subEntries" -> r.entries)

  implicit def toJValue(e: SubEntry): JValue = {
    ("number" -> e.number) ~
    ("start" -> e.start) ~
    ("stop" -> e.stop) ~
    ("text" -> e.text)
  }

  implicit def toJValue(d: Date): JValue = {
    JString(df.format(d))
  }
}
