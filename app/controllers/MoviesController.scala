package controllers

import javax.inject.{Inject, Singleton}

import persistence.PersistenceManager
import play.api.mvc.{Action, Controller}

@Singleton
class MoviesController @Inject()() extends Controller {

  def viewMovie = Action {
    Ok(views.html.movie())
  }

  def viewMovies = Action {
    Ok(views.html.movies())
  }
}
