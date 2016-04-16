package controllers

import javax.inject.{Inject, Singleton}

import model.{EpisodeType, IMovie, MovieType, Series}
import org.json4s.jackson.Serialization._
import persistence.PersistenceManager
import play.api.mvc.{Action, Controller}
import search.{SearchInteractor, SuggestionResult}
import subtitles.SeriesDetailProvider

@Singleton
class IMoviesController @Inject()(searchInteractor: SearchInteractor,
                                  persistenceManager: PersistenceManager,
                                  seriesDetailProvider: SeriesDetailProvider)
  extends Controller {

  implicit val formats = org.json4s.DefaultFormats

  def imovie(id: Int) = Action {
    persistenceManager.findIMovieById(id) match {
      case Some(imovie) => Ok(write(imovie)).as(JSON)
      case None => NotFound("")
    }
  }

  def viewImovies() = Action {
    Ok(views.html.imovies())
  }

  def imovies() = Action {
    Ok(write(IMoviesResponse(persistenceManager.listIMovies()))).as(JSON)
  }

  def suggest(query: String) = Action {
    if (query.isEmpty) {
      BadRequest("query is empty")
    } else {
      val suggestions = searchInteractor.suggestMovieFull("movie", query)
      val results = suggestions.map { s =>
        AutocompleteSuggestion(s.title, s)
      }
      Ok(write(IMovieSuggestion(query, results))).as(JSON)
    }
  }

  def addMovie(id: Int) = Action {
    persistenceManager.findIMovieById(id) match {
      case Some(imovie) =>
        imovie.titleType match {
          case MovieType =>
            persistenceManager.saveIMovieAsMovie(imovie)
            Ok("""{"info":"Movie is now part of the collection"}""").as(JSON)
          case Series =>
            seriesDetailProvider.get(imovie.imdbId.get)
            persistenceManager.saveIMovieAsSeries(imovie)
            Ok("""{"info":"Series is now part of the collection"}""").as(JSON)
          case EpisodeType =>
            Ok("""{"info":"Nothing to add"""")
        }
      case None => NotFound("""{"info":"Did not add movie"}""").as(JSON)
    }
  }
}

case class IMovieSuggestion(text: String, suggestions: List[AutocompleteSuggestion])
case class AutocompleteSuggestion(value: String, data: SuggestionResult)
case class IMoviesResponse(imovies: List[IMovie])

