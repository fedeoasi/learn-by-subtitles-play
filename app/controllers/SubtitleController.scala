package controllers

import javax.inject.{Inject, Singleton}

import play.api.mvc.{Action, Controller}
import services.SubtitlesService

@Singleton
class SubtitleController @Inject() (service: SubtitlesService) extends Controller {
  def viewSubtitle(imdbId: String) = Action {
    Ok(views.html.subtitles(imdbId))
  }

  def subtitle(imdbId: String) = Action {
    Ok(views.html.subtitles(imdbId))
  }
}
