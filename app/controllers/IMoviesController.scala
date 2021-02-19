package controllers

import javax.inject.{ Inject, Singleton }
import model.{ IMovie, MovieType, SeriesType }
import org.json4s.jackson.Serialization._
import persistence.PersistenceManager
import play.api.mvc.InjectedController
import search.{ SearchInteractor, SuggestionResult }
import serialization.JsonFormats
import subtitles.SeriesDetailProvider

@Singleton
class IMoviesController @Inject() (
    searchInteractor: SearchInteractor,
    persistenceManager: PersistenceManager,
    seriesDetailProvider: SeriesDetailProvider
) extends InjectedController {

  implicit val formats = JsonFormats

  def imovie(id: String) = Action {
    persistenceManager.findIMovieById(id) match {
      case Some(imovie) => Ok(write(imovie)).as(JSON)
      case None         => NotFound("")
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

  def addMovie(id: String) = Action {
    persistenceManager.findIMovieById(id) match {
      case Some(imovie) =>
        imovie.titleType match {
          case MovieType =>
            persistenceManager.saveIMovieAsMovie(imovie)
            Ok("""{"info":"Movie is now part of the collection"}""").as(JSON)
          case SeriesType =>
            seriesDetailProvider.get(imovie.imdbId)
            persistenceManager.saveIMovieAsSeries(imovie)
            Ok("""{"info":"Series is now part of the collection"}""").as(JSON)
          case _ =>
            Ok("""{"info":"Nothing to add"""")
        }
      case None => NotFound("""{"info":"Did not add movie"}""").as(JSON)
    }
  }
}

case class IMovieSuggestion(
    text: String,
    suggestions: List[AutocompleteSuggestion]
)
case class AutocompleteSuggestion(value: String, data: SuggestionResult)
case class IMoviesResponse(imovies: List[IMovie])
