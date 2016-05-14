package controllers

import java.text.SimpleDateFormat
import javax.inject.{Inject, Singleton}

import config.Config
import org.json4s.DefaultFormats
import parsing.SrtParser
import persistence.PersistenceManager
import play.api.mvc.{Action, Controller}
import search.{SearchInteractor, SearchSubtitleResult}
import org.json4s.jackson.Serialization._

@Singleton
class SearchController @Inject() (parser: SrtParser,
                                  persistenceManager: PersistenceManager,
                                  searchInteractor: SearchInteractor)
  extends Controller {

  implicit val formats = new DefaultFormats {
    override protected def dateFormatter: SimpleDateFormat = {
      val f = new SimpleDateFormat("HH:mm:ss.SSS")
      f.setTimeZone(DefaultFormats.UTC)
      f
    }
  }

  def viewSearch = Action {
    Ok(views.html.search())
  }

  def search(query: String) = Action {
    if (query.isEmpty) {
      NotFound("empty query string")
    } else {
      Ok(write(SearchResult(query, doSearch(query)))).as(JSON)
    }
  }

  private def doSearch(q: String): List[SearchSubtitleResult] = {
    val subtitles = searchInteractor.searchSubtitles(Config.indexName, q)
    val imdbIds = subtitles.map(_.movieId)
    val titlesByImdbId = findTitles(imdbIds)
    subtitles.map { result =>
      val entries = parser.parseWithinHighlighted(result.highlightedText, Config.searchResultOffset)
      SearchSubtitleResult(result.subtitleId, titlesByImdbId(result.movieId), result.score, entries)
    }
  }

  private def findTitles(imdbIds: Seq[String]): Map[String, String] = persistenceManager.titlesByImdbId(imdbIds)
}

case class SearchResult(query: String, results: Seq[SearchSubtitleResult])