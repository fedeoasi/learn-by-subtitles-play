package controllers

import javax.inject.{Inject, Singleton}

import play.api.mvc.{Action, Controller}

@Singleton
class MoviesController @Inject()() extends Controller {
  def movie = Action {
    Ok(views.html.movie())
  }

  def movies = Action {
    Ok(views.html.movies())
  }
}
