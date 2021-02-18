package imdb

import java.io.{BufferedInputStream, FileInputStream, InputStream}
import java.nio.file.{Path, Paths}
import java.util.zip.GZIPInputStream

import com.github.tototoshi.csv.{CSVReader, CSVWriter, DefaultCSVFormat, QUOTE_NONE, Quoting}
import omdb.TitleScorer

import scala.io.Source
import resource._

object DumpImporter {
  // The files used here are taken from https://m.imdb.com/interfaces/

  private val BasicsFile = Paths.get("/home/fcaimi/Downloads/title.basics.tsv.gz")
  private val RatingsFile = Paths.get("/home/fcaimi/Downloads/title.ratings.tsv.gz")
  private val MinVotes = 1000

  object Fields {
    val NumVotes = "numVotes"
    val AvgRating = "averageRating"
    val ImdbId = "tconst"

    val TitleType = "titleType"
    val Title = "originalTitle"
    val StartYear = "startYear"
    // sample entry: Map(runtimeMinutes -> \N, tconst -> tt0215145, titleType -> movie, originalTitle -> Sant Janabai,
    // startYear -> 1949, endYear -> \N, primaryTitle -> Saint Janabai, isAdult -> 0, genres -> \N)
  }

  def gzipStream(file: Path): InputStream = new GZIPInputStream(new BufferedInputStream(new FileInputStream(file.toFile.getAbsolutePath)))

  case class ImdbRating(imdb: String, votes: Int, vote: String)

  case class ImdbTitle(imdbId: String, title: String, rating: String, voteCount: Int, startYear: Option[String], titleType: String) {
    val toCsvLine: Seq[Any] = Seq(imdbId, title, rating, voteCount, startYear.getOrElse("-"), titleType)
  }

  case class ScoredTitle(score: Double, title: ImdbTitle) {
    val toCsvLine: Seq[Any] = title.toCsvLine :+ score
  }

  def main(args: Array[String]): Unit = {
    import Fields._
    val ratingsByImdb = managed(reader(RatingsFile)).acquireAndGet { basics =>
      basics.iteratorWithHeaders.filter(_(NumVotes).toInt > MinVotes).map { fields =>
        val id = fields(ImdbId)
        id -> ImdbRating(id, fields(NumVotes).toInt, fields(AvgRating))
      }.toMap
    }
    val titles = managed(reader(BasicsFile)).acquireAndGet { basics =>
      val iterator = basics.iteratorWithHeaders.filter { fields =>
        fields("isAdult") == "0" && ratingsByImdb.contains(fields(ImdbId))
      }
      iterator.map { fields =>
        val id = fields(ImdbId)
        val rating = ratingsByImdb(id)
        ImdbTitle(id, fields(Title), rating.vote, rating.votes, fields.getOpt(StartYear), fields(TitleType))
      }.toList
    }
    val scoredTitles = titles.map { title =>
      ScoredTitle(TitleScorer.computeScore(title.rating.toDouble, title.voteCount, 7.0), title)
    }

    managed(CSVWriter.open("imdb_titles.csv")).acquireAndGet { writer =>
      writer.writeAll(scoredTitles.map(_.toCsvLine))
    }
    managed(CSVWriter.open("imdb_movies.csv")).acquireAndGet { writer =>
      writer.writeAll(scoredTitles.filter(_.title.titleType == "movie").map(_.toCsvLine))
    }
  }

  private implicit class FieldsOps(fields: Map[String, String]) {
    def getOpt(fieldName: String): Option[String] = fields(fieldName) match {
      case "\\N" => None
      case other => Some(other)
    }
  }

  def reader(file: Path): CSVReader = {
    val source = Source.fromInputStream(gzipStream(file))
    val VeryRareCharacter = '|'
    CSVReader.open(source)(new DefaultCSVFormat {
      override val delimiter: Char = '\t'
      override val lineTerminator: String = "\n"
      override val quoting: Quoting = QUOTE_NONE
      override val quoteChar: Char = VeryRareCharacter
      override val escapeChar: Char = VeryRareCharacter
    })
  }
}
