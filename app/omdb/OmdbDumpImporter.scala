package omdb

import model.{ TitleType, IMovie }
import omdb.OmdbDumpFields._
import omdb.OmdbDumpUtils._
import persistence.ProdPersistenceManager

import scala.util.control.NonFatal

case class OmdbTitle(
    id: Long,
    imdbId: String,
    title: String,
    year: Int,
    runtime: String,
    genre: String,
    imdbRating: Option[Double],
    imdbVotes: Option[Int],
    poster: String,
    titleType: String
)

object OmdbDumpImporter {
  def main(args: Array[String]) = {
    if (args.length < 1) {
      println("Dump location required")
    } else {
      importDump(args(0))
    }
  }

  def importDump(location: String): Unit = {
    val pm = ProdPersistenceManager()
    val inputLines = dumpInputStreamLines(location)
    val indexByField = extractHeaderIndex(inputLines)
    val titles = inputLines.map { l =>
      val values = l.split(tabRegex)
      val id = values(indexByField(Id)).toLong
      val yearValue = values(indexByField(Year))
      val year =
        try {
          yearValue.toInt
        } catch {
          case NonFatal(ex) => yearValue.substring(0, 4).toInt
        }
      val imdbId = values(indexByField(ImdbId))
      val title = values(indexByField(Title))
      val runtime = values(indexByField(Runtime))
      val genre = values(indexByField(Genre))
      val imdbVotes = parseOptionalInt(values(indexByField(ImdbVotes)))
      val imdbRating = parseOptionalDouble(values(indexByField(ImdbRating)))
      val poster = values(indexByField(Poster))
      val titleType = values(indexByField(Type))
      OmdbTitle(
        id,
        imdbId,
        title,
        year,
        runtime,
        genre,
        imdbRating,
        imdbVotes,
        poster,
        titleType
      )
    }
    val moviesWithImdbVotes = titles.filter { om =>
      om.imdbRating.isDefined && om.imdbVotes.isDefined
    }.toSeq

    val avgVote = computeAverageVote(moviesWithImdbVotes)
    println(s"The average vote is $avgVote")
    val imoviesToInsert = moviesWithImdbVotes.map { om =>
      val score = TitleScorer.computeScore(om, avgVote)
      IMovie(
        om.id,
        om.title,
        om.year,
        BigDecimal(om.imdbRating.get),
        om.imdbVotes.get,
        score,
        om.genre,
        om.poster,
        TitleType(om.titleType),
        om.imdbId
      )
    }
    pm.saveIMovies(imoviesToInsert)
  }

  def computeAverageVote(titles: Seq[OmdbTitle]): Double = {
    val weightedSum = titles.map { m => m.imdbRating.get * m.imdbVotes.get }.sum
    val sumOfWeigths = titles.map { m => m.imdbVotes.get }.sum
    weightedSum / sumOfWeigths
  }

  def printTopK(titles: Array[OmdbTitle]): Unit = {
    val sorted = titles.sortBy(_.imdbRating.get)
    val topK = sorted.filter(_.genre != "Documentary").reverse.take(50)
    topK.foreach(println)
  }

  def printHeader(location: String): Unit = {
    val inputLines = dumpInputStreamLines(location)
    val header = extractHeader(inputLines)
    println(header.mkString("\t"))
  }
}
