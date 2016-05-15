package omdb

object TitleScorer {
  /*
      weighted rating (WR)=(v/(v+m))R+(m/(v+m))C
      where:
      R = average for the movie (mean) = (Rating)
      v = number of votes for the movie = (votes)
      m = minimum votes required to be listed in the Top 50 (currently 1000)
      C = the mean vote across the whole report (currently 6.8)
     */
  def computeScore(title: OmdbTitle, avgVote: BigDecimal): BigDecimal = {
    val R = BigDecimal(title.imdbRating.get)
    val v = BigDecimal(title.imdbVotes.get)
    val m = 1000
    val C = avgVote
    (v / (v + m)) * R + (m / (v + m)) * C
  }
}
