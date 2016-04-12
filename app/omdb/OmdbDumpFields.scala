package omdb

object OmdbDumpFields {
  val Id = "ID"
  val ImdbId = "imdbID"
  val ImdbRating = "imdbRating"
  val ImdbVotes = "imdbVotes"
  val Title = "Title"
  val Runtime = "Runtime"
  val Genre = "Genre"
  val Poster = "Poster"
  val LastUpdated = "lastUpdated"
  val Type = "Type"
  val Year = "Year"

  val interestingFields = Id :: ImdbId :: Year :: ImdbRating :: ImdbVotes :: Title :: Runtime :: Genre ::
    Poster :: LastUpdated :: Type :: Nil
}
