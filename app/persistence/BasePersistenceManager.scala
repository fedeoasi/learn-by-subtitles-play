package persistence

import logging.Logging
import org.joda.time.DateTime

import scala.slick.jdbc.StaticQuery
import scala.slick.jdbc.meta.MTable
import model._

abstract class BasePersistenceManager extends PersistenceManager with Logging {
  val dal: LearnBySubtitlesDAL
  import dal._
  import driver.simple._
  import CustomColumnTypes._

  val database: Database

  def initializeDatabase() {
    database withSession { implicit s =>
      dal.create
    }
    logger.info("The database has been initialized")
  }

  override def findSubtitlesToIndex(): List[Subtitle] = {
    val list = database withSession { implicit s =>
      val q = for {
        s <- subtitles if s.indexed === false
      } yield s
      q.list.map(s => Subtitle(s.id, s.imdbId))
    }
    list.filter { p =>
      val movie = findMovieById(p.imdbId)
      movie.orElse(findEpisodeById(p.imdbId)).isDefined
    }
  }

  override def markSubtitleAsIndexed(subId: String) {
    database withSession { implicit s =>
      val q = for(s <- subtitles if s.id === subId) yield s.indexed
      q.update(true)
    }
  }

  override def saveMovie(m: Movie) {
    if (findMovieById(m.imdbID).isEmpty) {
      database withSession { implicit s =>
        titles.insert(daoFromTitle(m))
      }
    }
  }

  override def saveSeries(s: Series): Unit = {
    if (findSeriesById(s.imdbID).isEmpty) {
      database withSession { implicit session =>
        titles.insert(daoFromTitle(s))
      }
    }
  }

  override def deleteMovie(imdbId: String): Int = {
    database withTransaction { implicit t =>
      titles.filter(_.imdbId === imdbId).delete
    }
  }

  def daoFromTitle(t: Title) = {
    t match {
      case m: Movie =>
        TitleDao(m.imdbID, Some(m.year), Some(m.title), MovieType.discriminator, Some(m.posterUrl), None, None, None, m.id)
      case s: Series =>
        TitleDao(s.imdbID, Some(s.year), Some(s.title), SeriesType.discriminator, Some(s.posterUrl), None, None, None, s.id)
      case e: Episode =>
        TitleDao(t.imdbID, None, None, EpisodeType.discriminator, None, Some(e.season), Some(e.number), Some(e.seriesImdbId), None)
    }
  }

  override def listMovies(): List[Movie] = {
    database withSession { implicit s =>
      val q = titles.filter(_.movieType === MovieType.discriminator)
      q.list.map(movieFromDao)
    }
  }

  override def listSeries(): List[Series] = {
    database.withSession { implicit s =>
      titles.filter(_.movieType === SeriesType.discriminator).list.map(seriesFromDao)
    }
  }

  def listSubtitles(): List[Subtitle] = {
    database withSession { implicit s =>
      subtitles.list.map(extractSubtitle)
    }
  }

  override def findMovieById(imdbId: String): Option[Movie] = {
    database withSession { implicit s =>
      val q = for {
        m <- titles if m.imdbId === imdbId && m.movieType === MovieType.discriminator
      } yield m
      q.list.headOption.map(movieFromDao)
    }
  }

  override def findTitleById(imdbId: String): Option[Title] = {
    database withSession { implicit s =>
      val q = titles.filter(_.imdbId === imdbId)
      q.list.headOption.map(titleFromDao)
    }
  }

  override def findSeriesById(imdbId: String): Option[Series] = {
    database withSession { implicit s =>
      val q = titles.filter(m => m.imdbId === imdbId && m.movieType === SeriesType.discriminator)
      q.list.headOption.map(seriesFromDao)
    }
  }
  
  private def movieFromDao(d: TitleDao): Movie = Movie(d.imdbID, d.year.get, d.title.get, d.posterUrl.get, d.id)
  private def seriesFromDao(d: TitleDao): Series = Series(d.imdbID, d.year.get, d.title.get, d.posterUrl.get, d.id)

  private def titleFromDao(m: TitleDao): Title = {
    TitleType.typesByDiscriminator(m.movieType) match {
      case MovieType => Movie(m.imdbID, m.year.get, m.title.get, m.posterUrl.get, m.id)
      case SeriesType => Series(m.imdbID, m.year.get, m.title.get, m.posterUrl.get, m.id)
      case EpisodeType => Episode(m.imdbID, m.season.get, m.number.get, m.seriesImdbId.get)
      case _ => throw new IllegalStateException(s"Unsupported title type for title $m")
    }
  }

  override def findSubtitleById(id: String): Option[Subtitle] = {
    database withSession { implicit s =>
      val q = for {
        s <- subtitles if s.id === id
      } yield s
      q.list.headOption.map(extractSubtitle)
    }
  }

  override def saveSubtitle(subtitle: Subtitle) {
    if (findSubtitleForMovie(subtitle.imdbId).isEmpty
      && findSubtitleById(subtitle.id).isEmpty) {
      database withSession { implicit s =>
        subtitles.insert(subtitleDao(subtitle))
      }
    }
  }

  private[this] def subtitleDao(s: Subtitle): SubtitleDao = SubtitleDao(s.id, s.imdbId, indexed = false)

  override def findSubtitleForMovie(imdbId: String): Option[Subtitle] = {
    database withSession { implicit s =>
      val q = for {
        s <- subtitles if s.imdbId === imdbId
      } yield s
      q.list.headOption.map(extractSubtitle)
    }
  }

  private[this] def extractSubtitle(dao: SubtitleDao): Subtitle = Subtitle(dao.id, dao.imdbId)

  override def saveEpisode(episode: Episode) {
    if (findEpisodeById(episode.imdbID).isEmpty) {
      database withSession { implicit s =>
        titles.insert(daoFromTitle(episode))
      }
    }
  }

  override def findEpisodeForSeries(imdbId: String, seasonNumber: Int, episodeNumber: Int): Option[Episode] = {
    database withSession { implicit s =>
      val q = for {
        e <- titles if e.seriesImdbId === imdbId && e.season === seasonNumber && e.number === episodeNumber
      } yield e
      q.list.headOption.map(episodeFromDao)
    }
  }

  override def findEpisodeById(id: String): Option[Episode] = {
    database withSession { implicit s =>
      val q = for {
        e <- titles if e.imdbId === id
      } yield e
      q.list.headOption.map(episodeFromDao)
    }
  }

  def episodeFromDao(dao: TitleDao): Episode = {
    Episode(dao.imdbID, dao.season.get, dao.number.get, dao.seriesImdbId.get)
  }

  def episodeFromTuple(episode: (String, Int, Int, String)): Episode = {
    Episode(episode._1, episode._2, episode._3, episode._4)
  }

  override def findEpisodesForSeries(imdbId: String): List[Episode] = {
    database withSession { implicit s =>
      val q = for {
        e <- titles if e.seriesImdbId === imdbId
      } yield e
      q.list.map(episodeFromDao)
    }
  }

  override def findEpisodesWithNoSubtitles(): List[Episode] = {
    database withSession { implicit s =>
      val query = "select IMDB_ID, SEASON, NUMBER, SERIES_IMDB_ID from TITLES e " +
        "where e.MOVIE_TYPE = 'e' and e.IMDB_ID not in (select IMDB_ID from SUBTITLES)"
      StaticQuery.queryNA[(String, Option[Int], Option[Int], Option[String])](query).list.map {
        case (imdbId, season, number, seriesImdbId) =>
          Episode(imdbId, season.get, number.get, seriesImdbId.get)
      }
    }
  }

  override def saveSubtitleDownload(): Unit = {
    val download = Download(DateTime.now)
    database.withSession { implicit s =>
      downloads.insert(download)
    }
  }

  override def subtitleDownloadsSince(time: DateTime): Int = {
    database.withSession { implicit s =>
      downloads.filter(_.time >= time).list.size
    }
  }

  override def nextAvailableDownload(size: Int, hours: Int): DateTime = {
    val topKDownloads = database withSession { implicit s =>
      downloads.sortBy(_.time.desc).take(size).list
    }
    if (topKDownloads.isEmpty) {
      DateTime.now
    } else {
      topKDownloads.last.time.plusHours(hours)
    }
  }


  override def saveDownloadError(subtitleId: String, imdbId: String, reason: String): Unit = {
    database.withSession { implicit s =>
      downloadErrors.insert(DownloadError(subtitleId, imdbId, DateTime.now, reason, None))
    }
  }

  override def downloadErrorsFor(imdbId: String): Seq[DownloadError] = {
    database.withSession { implicit s =>
      downloadErrors.filter(_.imdbId === imdbId).run
    }
  }

  override def lastKDownloadErrors(k: Int): Seq[DownloadError] = {
    database.withSession { implicit s =>
      downloadErrors.sortBy(_.time.desc).take(k).run
    }
  }

  override def listIMovies(): List[IMovie] = {
    database withSession { implicit s =>
      imovie.list
    }
  }

  override def saveIMovies(movies: Seq[IMovie]): Unit = {
    database withTransaction { implicit s =>
      movies.foreach(imovie.insert)
    }
  }

  override def saveIMovie(movie: IMovie): Unit = {
    database withSession { implicit s =>
      imovie.insert(movie)
    }
  }

  override def findIMovieById(id: String): Option[IMovie] = {
    database withSession { implicit s =>
      imovie.filter(_.imdbId === id).list.headOption
    }
  }

  override def titlesByImdbId(imdbIds: Seq[String]): Map[String, String] = {
    imdbIds.map { imdbId =>
      findTitleById(imdbId) match {
        case Some(title) =>
          title match {
            case m: Movie =>
              imdbId -> m.title
            case s: Series =>
              imdbId -> s.title
            case e: Episode =>
              val series = findSeriesById(e.seriesImdbId).get
              val title = series.title + " s" + e.season + "e" + e.number
              imdbId -> title
          }
        case None => throw new IllegalArgumentException(s"Could not find title with imdbId $imdbId")
      }
    }.toMap
  }

  override def saveIMovieAsMovie(movie: IMovie): Boolean = {
    tryToSaveMovie(movie)
  }

  override def saveIMovieAsSeries(series: IMovie): Boolean = {
    tryToSaveSeries(series)
  }

  def tryToSaveMovie(imovie: IMovie): Boolean = {
    findMovieById(imovie.imdbId) match {
      case Some(movie) => true
      case None =>
        saveMovie(toMovie(imovie))
        true
    }
  }


  def tryToSaveSeries(series: IMovie): Boolean = {
    findSeriesById(series.imdbId) match {
      case Some(movie) => true
      case None =>
        saveSeries(series.toSeries)
        true
    }
  }

  //TODO move this to IMovie or kill it
  private def toMovie(m: IMovie): Movie = {
    Movie(m.imdbId, m.year, m.title, m.poster, None)
  }
}