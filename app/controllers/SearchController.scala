package controllers

import javax.inject.{Inject, Singleton}

import play.api.mvc.{Action, Controller}

@Singleton
class SearchController @Inject() () extends Controller {
  def search = Action {
    Ok(views.html.search())
  }
}
