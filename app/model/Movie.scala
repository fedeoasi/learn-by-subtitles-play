package model

import java.util.Date

trait Title {
  def imdbID: String
  def isSeries: Boolean
  def isSimpleMovie: Boolean
  def id: Option[Int]
}

case class Movie(imdbID: String, year: Int, title: String, posterUrl: String, id: Option[Int]) extends Title {
  def isSeries: Boolean = false
  def isSimpleMovie: Boolean = true
}

case class SeriesTitle(imdbID: String, year: Int, title: String, posterUrl: String, id: Option[Int]) extends Title {
  def isSeries: Boolean = true
  def isSimpleMovie: Boolean = false
}

case class Episode(imdbID: String, season: Int, number: Int, seriesImdbId: String) extends Title {
  override def isSimpleMovie: Boolean = false
  override def isSeries: Boolean = false
  override def id: Option[Int] = ???
}

case class Season(parentImdbID: String, seasonNumber: Int, episodes: Int)
case class SubEntry(number: Int, start: Date, stop: Date, text: String)
case class Subtitle(id: String, imdbId: String)
case class SubtitleWithContent(subtitle: Subtitle, content: String)
case class IMovie(otherId: Long, title: String, year: Int, rating: BigDecimal, votes: Long, genre: String, poster: String, titleType: TitleType, imdbId: Option[String]) {
  def toTitle: Title = {
    titleType match {
      case MovieType => Movie(imdbId.get, year, title, poster, None)
      case Series => SeriesTitle(imdbId.get, year, title, poster, None)
      case EpisodeType => throw new IllegalArgumentException
    }
  }

  def toSeries: SeriesTitle = {
    require(titleType == Series)
    SeriesTitle(imdbId.get, year, title, poster, None)
  }
}

sealed trait TitleType {
  def discriminator: String
  def name: String
}

object TitleType {
  def apply(s: String): TitleType = typesByDiscriminator(s)

  val types = Set(MovieType, Series, EpisodeType)
  val typesByDiscriminator: Map[String, TitleType] = types.flatMap {
    t => Seq(t.discriminator -> t, t.name -> t)
  }.toMap
}

case object MovieType extends TitleType {
  override def discriminator: String = "m"
  override def name: String = "movie"
}
case object Series extends TitleType {
  override def discriminator: String = "s"
  override def name: String = "series"
}
case object EpisodeType extends TitleType {
  override def discriminator: String = "e"
  override def name: String = "episode"
}