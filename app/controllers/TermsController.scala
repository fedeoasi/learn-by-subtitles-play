package controllers

import javax.inject.Inject
import akka.actor.ActorSystem
import language.{Term, TermsProvider}
import play.api.mvc.{Action, Controller, InjectedController}
import serialization.JsonFormats
import org.json4s.jackson.Serialization._

import scala.concurrent.ExecutionContext

class TermsController @Inject() (actorSystem: ActorSystem,
                                 termsProvider: TermsProvider)
                                (implicit exec: ExecutionContext)
  extends InjectedController {

  implicit val formats = JsonFormats

  def all = Action.async {
    termsProvider.allTerms.map { terms =>
      Ok(write(TermsResponse(terms))).as(JSON)
    }
  }

  def random = Action.async {
    termsProvider.randomTerm.map { term =>
      Ok(write(TermResponse(term))).as(JSON)
    }
  }
}

case class TermsResponse(terms: Seq[Term])
case class TermResponse(term: Term)