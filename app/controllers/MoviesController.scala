package controllers

import javax.inject.{Inject, Singleton}

import model.Title
import org.json4s.jackson.Serialization._
import persistence.PersistenceManager
import play.api.mvc.{Action, Controller}

@Singleton
class MoviesController @Inject()(persistenceManager: PersistenceManager) extends Controller {
  implicit val formats = org.json4s.DefaultFormats

  def viewMovie = Action {
    Ok(views.html.movie())
  }

  def viewMovies = Action {
    Ok(views.html.movies())
  }

  def movies = Action {
    val movies = persistenceManager.listMovies()
    Ok(write(MoviesResponse(serializeTitles(movies)))).as(JSON)
  }

  def deleteMovie(imdbId: String) = Action {
    val result = persistenceManager.deleteMovie(imdbId)
    Ok(s"Deleted $result movie")
  }

  def viewSeries = Action {
    Ok(views.html.series())
  }

  def series = Action {
      val movies = persistenceManager.listSeries()
      Ok(write(SeriesResponse(serializeTitles(movies)))).as(JSON)
  }

  private def serializeTitles(movies: List[Title]): List[SerializedTitle] = {
    movies.map { m =>
      val posterUrl = "<img src='" + m.posterUrl + "' />"
      SerializedTitle(m.imdbID, m.year, m.title, posterUrl)
    }
  }
}

case class MoviesResponse(movies: List[SerializedTitle])
case class SeriesResponse(series: List[SerializedTitle])
case class SerializedTitle(imdbID: String, year: Int, title: String, posterUrl: String)
