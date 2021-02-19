package omdb

object TitleScorer {
  def computeScore(title: OmdbTitle, avgVote: Double): Double = {
    val result = for {
      rating <- title.imdbRating
      votes <- title.imdbVotes
    } yield computeScore(rating, votes, avgVote)
    result.getOrElse {
      throw new RuntimeException(
        s"title $title needs to provide score and number of votes"
      )
    }
  }

  /** Computes the weighted rating (WR)=(v/(v+m))R+(m/(v+m))C
    *  where:
    *  R = average for the movie (mean) = (Rating)
    *  v = number of votes for the movie = (votes)
    *  m = minimum votes required to be listed in the Top 50 (currently 1000)
    *  C = the mean vote across the whole report (currently 6.8)
    */
  def computeScore(rating: Double, votes: Int, avgVote: Double): Double = {
    val R = rating
    val v = votes.toDouble
    val m = 1000
    val C = avgVote
    (v / (v + m)) * R + (m / (v + m)) * C
  }
}
