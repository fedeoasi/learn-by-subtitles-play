package controllers

import javax.inject.{Inject, Singleton}
import model.{Movie, Series}
import org.json4s.jackson.Serialization._
import persistence.PersistenceManager
import play.api.mvc.InjectedController
import serialization.JsonFormats

@Singleton
class MoviesController @Inject()(persistenceManager: PersistenceManager) extends InjectedController {
  implicit val formats = JsonFormats

  def viewMovie(search: Option[String]) = Action {
    Ok(views.html.movie(search))
  }

  def viewMovies = Action {
    Ok(views.html.movies())
  }

  def movies = Action {
    val movies = persistenceManager.listMovies()
    Ok(write(MoviesResponse(serializeMovies(movies)))).as(JSON)
  }

  def deleteMovie(imdbId: String) = Action {
    val result = persistenceManager.deleteMovie(imdbId)
    Ok(s"Deleted $result movie")
  }

  def viewSeries = Action {
    Ok(views.html.series())
  }

  def series = Action {
      val series = persistenceManager.listSeries()
      Ok(write(SeriesResponse(serializeSeries(series)))).as(JSON)
  }

  private def serializeMovies(movies: List[Movie]): List[SerializedTitle] = {
    movies.map { m =>
      val posterUrl = "<img src='" + m.posterUrl + "' />"
      SerializedTitle(m.imdbID, m.year, m.title, posterUrl)
    }
  }

  private def serializeSeries(series: List[Series]): List[SerializedTitle] = {
    series.map { s =>
      val posterUrl = "<img src='" + s.posterUrl + "' />"
      SerializedTitle(s.imdbID, s.year, s.title, posterUrl)
    }
  }
}

case class MoviesResponse(movies: List[SerializedTitle])
case class SeriesResponse(series: List[SerializedTitle])
case class SerializedTitle(imdbID: String, year: Int, title: String, posterUrl: String)
