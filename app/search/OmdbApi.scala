package search

import dispatch.Defaults._
import dispatch._
import logging.Logging
import model.{ Movie, Title, TitleType }
import org.json4s._
import org.json4s.jackson.JsonMethods._
import serialization.JsonFormats

trait MovieSearcher {
  def searchTitleJson(title: String): String
  def searchTitle(title: String): Option[Title]
}

class OmdbApi extends MovieSearcher with Logging {
  implicit val formats = JsonFormats + new TitleTypeSerializer

  def searchTitleJson(title: String): String = {
    val request = url("http://www.omdbapi.com") <<? Map("t" -> title)
    val responseString = Http.default(request OK as.String)
    val jsonString = responseString()
    logger.debug("json: " + jsonString)
    jsonString
  }

  private def extractYear(s: String): BigInt = {
    BigInt(s.substring(0, 4))
  }

  def searchTitle(title: String): Option[Movie] = {
    val jsonString = searchTitleJson(title)
    parseMovie(jsonString)
  }

  def parseMovie(jsonString: String): Option[Movie] = {
    val json = parse(jsonString)
    val modified = json transformField {
      case ("Title", x)  => ("title", x)
      case ("Year", x)   => ("year", JInt(extractYear(x.extract[String])))
      case ("Poster", x) => ("posterUrl", x)
      case ("Type", x)   => ("movieType", x)
    }
    try {
      Some(modified.extract[Movie])
    } catch {
      case e: Throwable =>
        logger.error("Unable to parse movie json: " + jsonString, e)
        None
    }
  }
}

class TitleTypeSerializer
    extends CustomSerializer[TitleType](format =>
      (
        { case JString(s) => TitleType.typesByDiscriminator(s) },
        { case t: TitleType => JString(t.discriminator) }
      )
    )
