package tasks

import persistence.ProdPersistenceManager
import search.ElasticSearchInteractor
import config.Config

object IndexIMoviesTask {
  def main(args: Array[String]): Unit = {
    val persistenceManager = ProdPersistenceManager()
    val searchInteractor = new ElasticSearchInteractor
    searchInteractor.ensureMovieIndex(Config.movieIndexName)
    val toBeIndexed = persistenceManager.listIMovies()
    println(s"Found ${toBeIndexed.size} IMovies to index")
    toBeIndexed.zipWithIndex.foreach { case (m, i) =>
      searchInteractor.indexIMovie(Config.movieIndexName, m, flush = false)
    }
  }
}
