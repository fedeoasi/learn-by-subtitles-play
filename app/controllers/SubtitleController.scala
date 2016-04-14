package controllers

import javax.inject.{Inject, Singleton}

import model.SubEntry
import play.api.mvc.{Action, Controller}
import services.SubtitlesService
import org.json4s.jackson.Serialization._

@Singleton
class SubtitleController @Inject() (service: SubtitlesService) extends Controller {
  implicit val formats = org.json4s.DefaultFormats

  def viewSubtitle(imdbId: String) = Action {
    Ok(views.html.subtitles(imdbId))
  }

  def subtitle(imdbId: String) = Action {
    val subtitles = service.getSubtitlesForMovie(imdbId)
    Ok(write(SubtitlesResponse(subtitles))).as(JSON)
  }
}

case class SubtitlesResponse(subEntries: Seq[SubEntry])
