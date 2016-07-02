package controllers

import javax.inject.Inject

import language.TermsProvider
import play.api.mvc.{Action, Controller}
import serialization.JsonFormats
import org.json4s.jackson.Serialization._

class TermsController @Inject() (termsProvider: TermsProvider)
  extends Controller {

  implicit val formats = JsonFormats

  def all = Action {
    val terms = termsProvider.terms
    Ok(write(terms)).as(JSON)
  }
}
