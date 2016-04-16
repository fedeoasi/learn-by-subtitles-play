package persistence

import java.sql.Timestamp

import model._
import org.joda.time.{DateTime, DateTimeZone}

import scala.slick.driver.JdbcDriver

trait Profile {
  val driver: JdbcDriver
}

trait DBComponent {
  this: Profile =>
  import driver.simple._

  lazy val onDeleteAction: ForeignKeyAction = ForeignKeyAction.Cascade

  abstract class TableWithId[T](tag: Tag, tableName: String) extends Table[T](tag, tableName) {
    def id = column[Int]("ID", O.PrimaryKey, O.AutoInc, O.NotNull)
  }

//  class AnyTableQuery[DAO <: AnyDao, E <: AnyTable[DAO]](cons: Tag => E) extends TableQuery(cons) {
//    private lazy val insertInvoker = this returning this.map(_.id)
//
//    def insertAndGetId(dao: DAO)(implicit session: Session) : Int = {
//      insertInvoker += dao
//    }
//  }
}

case class TitleDao(imdbID: String,
                    year: Option[Int],
                    title: Option[String],
                    movieType: String,
                    posterUrl: Option[String],
                    season: Option[Int],
                    number: Option[Int],
                    seriesImdbId: Option[String],
                    id: Option[Int])
case class SubtitleDao(id: String, imdbId: String, indexed: Boolean)

trait LearnBySubtitlesDbComponent extends DBComponent {
  this: Profile =>
  import driver.simple._
  import CustomColumnTypes._

  object CustomColumnTypes {
    implicit lazy val jodaType = MappedColumnType.base[DateTime, Timestamp](
      {d => new Timestamp(d.getMillis)} ,
      {d => new DateTime(d.getTime, DateTimeZone.UTC)}
    )
  }

  class Titles(tag: Tag) extends TableWithId[TitleDao](tag, "TITLES") {
    def imdbId = column[String]("IMDB_ID")
    def title = column[Option[String]]("TITLE")
    def year = column[Option[Int]]("YEAR")
    def movieType = column[String]("MOVIE_TYPE")
    def posterUrl = column[Option[String]]("POSTER_URL")
    def season = column[Option[Int]]("SEASON")
    def number = column[Option[Int]]("NUMBER")
    def seriesImdbId = column[Option[String]]("SERIES_IMDB_ID")
    def * = (imdbId, year, title, movieType, posterUrl, season, number, seriesImdbId, id.?) <> (TitleDao.tupled, TitleDao.unapply)
  }

  object titles extends TableQuery(new Titles(_))

  class Subtitles(tag: Tag) extends Table[SubtitleDao](tag, "SUBTITLES") {
    def id = column[String]("ID", O.PrimaryKey)
    def imdbId = column[String]("IMDB_ID")
    def indexed = column[Boolean]("INDEXED")
    def * = (id, imdbId, indexed) <> (SubtitleDao.tupled, SubtitleDao.unapply)
  }

  object subtitles extends TableQuery(new Subtitles(_))

  case class Download(time: DateTime)

  class Downloads(tag: Tag) extends Table[Download](tag, "DOWNLOADS") {
    def time = column[DateTime]("TIME")
    def * = time <> ({ (time: DateTime) => Download(time) }, Download.unapply)
  }

  object downloads extends TableQuery(new Downloads(_))

  implicit val titleTypeColumnType = MappedColumnType.base[TitleType, String](
    { tt => tt.discriminator },    // map Bool to Int
    { s => TitleType(s) } // map Int to Bool
  )

  class IMovies(tag:Tag) extends Table[IMovie](tag, "IMOVIE") {
    def otherId = column[Long]("OTHER_ID")
    def title = column[String]("TITLE")
    def year = column[Int]("YEAR")
    def rating = column[BigDecimal]("RATING")
    def votes = column[Long]("VOTES")
    def genre = column[String]("GENRE")
    def poster = column[String]("POSTER")
    def imdbId = column[String]("IMDB_ID")
    def titleType = column[TitleType]("TYPE")
    def * = (otherId, title, year, rating, votes, genre, poster, titleType, imdbId.?) <> (IMovie.tupled, IMovie.unapply)
  }

  object imovie extends TableQuery(new IMovies(_))

  val tables = Seq(titles, subtitles, downloads, imovie)

  def createStatements = tables.map(_.ddl.createStatements.mkString("\n"))
}

class LearnBySubtitlesDAL(override val driver: JdbcDriver) extends LearnBySubtitlesDbComponent with Profile