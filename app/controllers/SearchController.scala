package controllers

import config.Config
import javax.inject.{ Inject, Singleton }
import org.json4s.jackson.Serialization._
import parsing.SrtParser
import persistence.PersistenceManager
import play.api.mvc.InjectedController
import search.{ SearchInteractor, SearchSubtitleResult }
import serialization.JsonFormats

@Singleton
class SearchController @Inject() (
    parser: SrtParser,
    persistenceManager: PersistenceManager,
    searchInteractor: SearchInteractor
) extends InjectedController {

  implicit val formats = JsonFormats

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
      val entries = parser.parseWithinHighlighted(
        result.highlightedText,
        Config.searchResultOffset
      )
      SearchSubtitleResult(
        result.subtitleId,
        titlesByImdbId(result.movieId),
        result.score,
        entries
      )
    }
  }

  private def findTitles(imdbIds: Seq[String]): Map[String, String] =
    persistenceManager.titlesByImdbId(imdbIds)
}

case class SearchResult(query: String, results: Seq[SearchSubtitleResult])
