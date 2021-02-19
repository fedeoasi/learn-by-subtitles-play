package search

import model.Title
import persistence.PersistenceManager

class LocalMovieSearcher(
    persistenceManager: PersistenceManager,
    elasticSearchInteractor: SearchInteractor,
    imovieIndexName: String
) extends MovieSearcher {

  override def searchTitleJson(title: String): String = ???

  override def searchTitle(title: String): Option[Title] = {
    val list = elasticSearchInteractor.suggestMovieFull(imovieIndexName, title)
    list.headOption.flatMap { t =>
      val id = t.id
      val optionalMovie = persistenceManager.findIMovieById(id)
      optionalMovie.map(_.toTitle)
    }
  }
}
