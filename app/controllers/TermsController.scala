package controllers

import javax.inject.Inject

import akka.actor.ActorSystem
import language.TermsProvider
import play.api.mvc.{Action, Controller}
import serialization.JsonFormats
import org.json4s.jackson.Serialization._

import scala.concurrent.ExecutionContext

class TermsController @Inject() (actorSystem: ActorSystem,
                                 termsProvider: TermsProvider)
                                (implicit exec: ExecutionContext)
  extends Controller {

  implicit val formats = JsonFormats

  def all = Action.async {
    termsProvider.allTerms.map { terms =>
      Ok(write(terms)).as(JSON)
    }
  }

  def random = Action.async {
    termsProvider.randomTerm.map { term =>
      Ok(write(term)).as(JSON)
    }
  }
}
