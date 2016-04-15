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
      if(!MTable.getTables.list.exists(_.name.name == movies.shaped.value.tableName)) {
        logger.info(movies.ddl.createStatements.mkString("\n"))
        movies.ddl.create
      }
      if(!MTable.getTables.list.exists(_.name.name == episodes.shaped.value.tableName)) {
        logger.info(episodes.ddl.createStatements.mkString("\n"))
        episodes.ddl.create
      }
      if(!MTable.getTables.list.exists(_.name.name == subtitles.shaped.value.tableName)) {
        logger.info(subtitles.ddl.createStatements.mkString("\n"))
        subtitles.ddl.create
      }
      if(!MTable.getTables.list.exists(_.name.name == downloads.shaped.value.tableName)) {
        logger.info(downloads.ddl.createStatements.mkString("\n"))
        downloads.ddl.create
      }
      if(!MTable.getTables.list.exists(_.name.name == imovie.shaped.value.tableName)) {
        logger.info(imovie.ddl.createStatements.mkString("\n"))
        imovie.ddl.create
      }
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
        movies.insert(daoFromTitle(m))
      }
    }
  }

  override def saveSeries(s: SeriesTitle): Unit = {
    if (findSeriesById(s.imdbID).isEmpty) {
      database withSession { implicit session =>
        movies.insert(daoFromTitle(s))
      }
    }
  }

  override def deleteMovie(imdbId: String): Int = {
    database withTransaction { implicit t =>
      movies.filter(_.imdbId === imdbId).delete
    }
  }

  def daoFromTitle(t: Title) = {
    val mt = if(t.isSimpleMovie) {
      MovieType
    } else if(t.isSeries) {
      Series
    } else {
      throw new IllegalStateException("Unknown movie type")
    }
    MovieDao(t.imdbID, t.year, t.title, mt.discriminator, t.posterUrl, None)
  }

  override def listMovies(): List[Movie] = {
    database withSession { implicit s =>
      val q = movies.filter(_.movieType === MovieType.discriminator)
      q.list.map(movieFromDao)
    }
  }

  override def listSeries(): List[SeriesTitle] = {
    database.withSession { implicit s =>
      movies.filter(_.movieType === Series.discriminator).list.map(seriesFromDao)
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
        m <- movies if m.imdbId === imdbId && m.movieType === MovieType.discriminator
      } yield m
      q.list.headOption.map(movieFromDao)
    }
  }

  override def findTitleById(imdbId: String): Option[Title] = {
    database withSession { implicit s =>
      val q = movies.filter(_.imdbId === imdbId)
      q.list.headOption.map(titleFromDao)
    }
  }

  override def findSeriesById(imdbId: String): Option[SeriesTitle] = {
    database withSession { implicit s =>
      val q = movies.filter(m => m.imdbId === imdbId && m.movieType === Series.discriminator)
      q.list.headOption.map(seriesFromDao)
    }
  }
  
  private def movieFromDao(d: MovieDao): Movie = Movie(d.imdbID, d.year, d.title, d.posterUrl, d.id)
  private def seriesFromDao(d: MovieDao): SeriesTitle = SeriesTitle(d.imdbID, d.year, d.title, d.posterUrl, d.id)

  private def titleFromDao(m: MovieDao): Title = {
    TitleType.typesByDiscriminator(m.movieType) match {
      case MovieType => Movie(m.imdbID, m.year, m.title, m.posterUrl, m.id)
      case Series => SeriesTitle(m.imdbID, m.year, m.title, m.posterUrl, m.id)
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
        episodes.insert(episode)
      }
    }
  }

  override def findEpisodeForSeries(imdbId: String, seasonNumber: Int, episodeNumber: Int): Option[Episode] = {
    database withSession { implicit s =>
      val q = for {
        e <- episodes if e.seriesImdbId === imdbId && e.season === seasonNumber && e.number === episodeNumber
      } yield e
      q.list.headOption
    }
  }

  override def findEpisodeById(id: String): Option[Episode] = {
    database withSession { implicit s =>
      val q = for {
        e <- episodes if e.imdbID === id
      } yield e
      q.list.headOption
    }
  }

  def episodeFromTuple(episode: (String, Int, Int, String)): Episode = {
    Episode(episode._1, episode._2, episode._3, episode._4)
  }

  override def findEpisodesForSeries(imdbId: String): List[Episode] = {
    database withSession { implicit s =>
      val q = for {
        e <- episodes if e.seriesImdbId === imdbId
      } yield e
      q.list
    }
  }

  override def findEpisodesWithNoSubtitles(): List[Episode] = {
    database withSession { implicit s =>
      StaticQuery.queryNA[(String, Int, Int, String)]("select * from EPISODES e where e.IMDB_ID not in " +
        "(select IMDB_ID from SUBTITLES)").list.map(Episode.tupled)
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
    topKDownloads.last.time.plusHours(hours)
  }

  override def listIMovies(): List[IMovie] = {
    database withSession { implicit s =>
      imovie.list
    }
  }

  override def saveIMovie(movie: IMovie): Unit = {
    //TODO incremental approach
    database withSession { implicit s =>
      imovie.insert(movie)
    }
  }

  override def findIMovieById(id: Int): Option[IMovie] = {
    database withSession { implicit s =>
      imovie.filter(_.otherId === id.toLong).list.headOption
    }
  }

  override def titlesByImdbId(imdbIds: Seq[String]): Map[String, String] = {
    imdbIds.map { imdbId =>
      findTitleById(imdbId) match {
        case Some(m) => imdbId -> m.title
        case None =>
          findEpisodeById(imdbId) match {
            case Some(e) => imdbId -> {
              val series = findSeriesById(e.seriesImdbId).get
              series.title + " s" + e.season + "e" + e.number
            }
            case None => throw new IllegalArgumentException("No movies or episodes with imdbId " + imdbId)
          }
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
    findMovieById(imovie.imdbId.get) match {
      case Some(movie) => true
      case None =>
        saveMovie(toMovie(imovie))
        true
    }
  }


  def tryToSaveSeries(series: IMovie): Boolean = {
    findSeriesById(series.imdbId.get) match {
      case Some(movie) => true
      case None =>
        saveSeries(series.toSeries)
        true
    }
  }

  //TODO move this to IMovie or kill it
  private def toMovie(m: IMovie): Movie = {
    Movie(m.imdbId.get, m.year, m.title, m.poster, None)
  }
}