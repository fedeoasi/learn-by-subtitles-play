package subtitles

import java.io.{FileOutputStream, StringReader, File}
import utils.Utils._
import model.Subtitle
import org.apache.commons.io.IOUtils

trait SubtitleStorage {
  def retrieveSubtitleContent(s: Subtitle): String
  def persistSubtitleFile(subtitle: Subtitle, subtitleString: String): Unit
}

class LocalSubtitleStorage extends SubtitleStorage {
  val subtitlesFolder = "subtitleFiles"

  def retrieveSubtitleContent(s: Subtitle): String = readFileIntoString(getSubtitleFileLocation(s.id))

  def persistSubtitleFile(subtitle: Subtitle, subtitleString: String): Unit = {
    require(!subtitle.id.isEmpty)
    IOUtils.copy(new StringReader(subtitleString), new FileOutputStream(getSubtitleFileLocation(subtitle.id)))
  }

  private def getSubtitleFileLocation(id: String): String =
    s"$subtitlesFolder${File.separator}$id.srt"
}
