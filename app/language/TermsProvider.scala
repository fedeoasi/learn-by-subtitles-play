package language

import com.google.inject.Inject
import org.elasticsearch.search.aggregations.bucket.terms.{StringTerms, TermsBuilder}
import search.ElasticSearchInteractor
import scala.collection.JavaConverters._


case class Term(value: String, docCount: Long)

class TermsProvider @Inject() () extends ElasticSearchInteractor {
  private[this] val TermsAggregationName = "text_terms"

  private[this] val Limit = 1000000

  def terms: Seq[Term] = {

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


}
