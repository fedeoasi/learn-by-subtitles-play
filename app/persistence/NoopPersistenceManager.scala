package persistence

import org.joda.time.DateTime
import model._

class NoopPersistenceManager extends PersistenceManager {
  override def saveMovie(movie: Movie) {}

  override def findMovieById(imdbId: String): Option[Movie] = None

  override def listMovies(): List[Movie] = List[Movie]()

  override def listSeries(): List[Series] = ???

  override def deleteMovie(imdbId: String): Int = ???

  override def saveSubtitle(subtitle: Subtitle) {}

  override def findSubtitleForMovie(imdbId: String): Option[Subtitle] = None

  override def findSubtitleById(id: String): Option[Subtitle] = None

  override def findSubtitlesToIndex(): List[Subtitle] = List[Subtitle]()

  override def markSubtitleAsIndexed(subId: String) {}

  override def saveEpisode(episode: Episode) {}

  override def findEpisodeForSeries(imdbId: String, seasonNumber: Int, episodeNumber: Int): Option[Episode] = None

  override def findEpisodeById(id: String): Option[Episode] = None

  override def findEpisodesForSeries(imdbId: String): List[Episode] = List[Episode]()

  override def findEpisodesWithNoSubtitles(): List[Episode] = List[Episode]()

  override def saveSubtitleDownload(): Unit = {}

  override def subtitleDownloadsSince(time: DateTime): Int = 0

  override def nextAvailableDownload(size: Int, hours: Int): DateTime = ???

  override def listIMovies(): List[IMovie] = ???

  override def saveIMovie(movie: IMovie): Unit = ???

  override def saveIMovies(movie: Seq[IMovie]): Unit = ???

  override def titlesByImdbId(imdbIds: Seq[String]): Map[String, String] = ???

  override def findIMovieById(id: Int): Option[IMovie] = ???

  override def saveIMovieAsMovie(movie: IMovie): Boolean = ???

  override def saveIMovieAsSeries(series: IMovie): Boolean = ???

  override def findSeriesById(imdbId: String): Option[Series] = ???

  override def saveSeries(movie: Series): Unit = ???

  override def findTitleById(imdbId: String): Option[Title] = ???
}
