package controllers

import javax.inject._
import play.api._
import play.api.mvc._

@Singleton
class HomeController @Inject() extends InjectedController {
  def index = Action {
    Ok(views.html.index())
  }
}
