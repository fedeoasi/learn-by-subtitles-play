package omdb

import org.scalatest.{ FunSpec, Matchers }

/** Created by fcaimi on 5/14/16.
  */
class TitleScorerTest extends FunSpec with Matchers {
  import TitleScorer._

  it("computes the score") {
    computeScore(title(9, 1000), 7) shouldBe 8
  }

  def title(rating: Double, votes: Int): OmdbTitle = {
    new OmdbTitle(1, "asd", "", 2000, "", "", Some(rating), Some(votes), "", "")
  }

}
