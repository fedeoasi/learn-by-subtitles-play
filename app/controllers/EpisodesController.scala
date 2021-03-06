package controllers

import com.google.inject.{ Inject, Singleton }
import model.{ Episode, Series }
import org.json4s.jackson.Serialization._
import persistence.PersistenceManager
import play.api.mvc.InjectedController
import serialization.JsonFormats

@Singleton
class EpisodesController @Inject() (persistenceManager: PersistenceManager) extends InjectedController {
  implicit val formats = JsonFormats

  def viewEpisodes(imdbId: String) = Action {
    Ok(views.html.episodes(imdbId))
  }

  def episodes(imdbId: String) = Action {
    persistenceManager.findSeriesById(imdbId) match {
      case Some(series) =>
        val episodes = persistenceManager.findEpisodesForSeries(imdbId)
        Ok(write(EpisodesResponse(series, episodes)))
      case None =>
        NotFound(s"No series with imdbId $imdbId")
    }
  }
}

case class EpisodesResponse(series: Series, episodes: List[Episode])
