package persistence

import org.joda.time.DateTime
import model._

trait TitlePersistence {
  //TODO Unify save into saveTitle?
  def saveMovie(movie: Movie)
  def findMovieById(imdbId: String): Option[Movie]
  def deleteMovie(imdbId: String): Int
  def listMovies(): List[Movie]
  def saveSeries(movie: Series)
  def findSeriesById(imdbId: String): Option[Series]
  //TODO Deletion for Series?
  def listSeries(): List[Series]
  def saveEpisode(episode: Episode)
  def findEpisodeForSeries(
    imdbId: String,
    seasonNumber: Int,
    episodeNumber: Int
  ): Option[Episode]
  def findEpisodesForSeries(imdbId: String): List[Episode]
  def findEpisodeById(id: String): Option[Episode]
  def findTitleById(imdbId: String): Option[Title]
  def titlesByImdbId(imdbIds: Seq[String]): Map[String, String]
}

trait SubtitlePersistence {
  def listSubtitles(): List[Subtitle]
  def saveSubtitle(subtitle: Subtitle)
  def findSubtitleForMovie(imdbId: String): Option[Subtitle]
  def findSubtitleById(id: String): Option[Subtitle]
  def findSubtitlesToIndex(): List[Subtitle]
  def markSubtitleAsIndexed(subId: String)
  def findEpisodesWithNoSubtitles(): List[Episode]
}

trait SubtitleDownloadPersistence {
  def saveSubtitleDownload(): Unit
  def subtitleDownloadsSince(time: DateTime): Int
  def nextAvailableDownload(size: Int, hours: Int): DateTime
  def saveDownloadError(subtitleId: String, imdbId: String, status: String)
  def downloadErrorsFor(imdbId: String): Seq[DownloadError]
  def lastKDownloadErrors(k: Int): Seq[DownloadError]
}

trait IMoviePersistence {
  def listIMovies(): List[IMovie]
  def findIMovieById(id: String): Option[IMovie]
  def saveIMovies(movie: Seq[IMovie]): Unit
  def saveIMovie(movie: IMovie): Unit
  def saveIMovieAsMovie(movie: IMovie): Boolean
  def saveIMovieAsSeries(series: IMovie): Boolean
}

trait PersistenceManager
    extends TitlePersistence
    with SubtitlePersistence
    with IMoviePersistence
    with SubtitleDownloadPersistence
