package controllers

import javax.inject.{Inject, Singleton}

import org.json4s.jackson.Serialization._
import persistence.PersistenceManager
import play.api.mvc.{Action, Controller}
import search.{SearchInteractor, SuggestionResult}

@Singleton
class IMoviesController @Inject()(searchInteractor: SearchInteractor,
                                  persistenceManager: PersistenceManager)
  extends Controller {

  implicit val formats = org.json4s.DefaultFormats

  def imovie(id: Int) = Action {
    persistenceManager.findIMovieById(id) match {
      case Some(imovie) => Ok(write(imovie)).as(JSON)
      case None => NotFound("")
    }
  }

  def suggest(query: String) = Action {
    if (query.isEmpty) {
      BadRequest("query is empty")
    } else {
      val suggestions = searchInteractor.suggestMovieFull("movie", query)
      val results = suggestions.map { s =>
        AutocompleteSuggestion(s.title, s)
      }
      Ok(write(IMovieSuggestion(query, results)))
    }
  }
}

case class IMovieSuggestion(text: String, suggestions: List[AutocompleteSuggestion])
case class AutocompleteSuggestion(value: String, data: SuggestionResult)

