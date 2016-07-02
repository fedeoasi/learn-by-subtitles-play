package language

import com.google.common.cache.CacheBuilder
import com.google.inject.Inject
import org.elasticsearch.search.aggregations.bucket.terms.{StringTerms, TermsBuilder}
import search.ElasticSearchInteractor

import scala.collection.JavaConverters._
import scalacache._
import guava._
import memoization._
import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration._
import scala.util.Random

case class Term(value: String, docCount: Long)

class TermsProvider @Inject() (implicit exec: ExecutionContext)
  extends ElasticSearchInteractor {

  private[this] val TermsAggregationName = "text_terms"

  private[this] val Limit = 1000000

  private[this] implicit val cache = ScalaCache(GuavaCache())

  private[this] val random = new Random()

  def allTerms: Future[Seq[Term]] = {
    memoize(1.hour) {
      Future.successful(extractTerms)
    }
  }

  def extractTerms: Seq[Term] = {
    val search = client.prepareSearch("my-index")
      .setTypes("subtitle")
      .addField("_id")
      .addAggregation(new TermsBuilder(TermsAggregationName)
        .field("text")
        .size(1000000)
      )

    val result = search.get()

    val termsAggregation = result.getAggregations.get[StringTerms](TermsAggregationName)
    val terms = termsAggregation.getBuckets.asScala.take(Limit).map { b =>
      Term(b.getKey.toString, b.getDocCount)
    }
    terms.filter { t =>
      t.value.forall(_.isLetter)
    }
  }

  def randomTerm: Future[Term] = {
    allTerms.map { terms =>
      val randomIndex = random.nextInt(terms.size)
      terms(randomIndex)
    }
  }
}
