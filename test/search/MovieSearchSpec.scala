package search

import java.util.UUID

import logging.Logging
import model._
import org.scalatest.{FunSpec, Matchers}
import persistence.WithPersistenceManager

class MovieSearchSpec extends FunSpec with Matchers with Logging with WithPersistenceManager {
  private val esi = new ElasticSearchInteractor
  val imovieIndexName = s"test-imovie-${UUID.randomUUID().toString}"
  esi.ensureMovieIndex(imovieIndexName)
  private val ms = new LocalMovieSearcher(persistenceManager, esi, imovieIndexName)
  val firstIMovie = IMovie(1, "Movie One", 2000, BigDecimal(9), 1000, "Action", "p1", MovieType, Some("abc"))
  val secondIMovie = IMovie(2, "Movie Two", 2005, BigDecimal(79), 2000, "Drama", "p2", MovieType, Some("bcd"))
  val thirdIMovie = IMovie(3, "Series One", 2003, BigDecimal(80), 2001, "Drama", "p3", Series, Some("cde"))

  val firstMovie = Movie("abc", 2000, "Movie One", "p1", None)
  val secondMovie = Movie("bcd", 2005, "Movie Two", "p2", None)
  val firstSeries = SeriesTitle("cde", 2003, "Series One", "p3", None)

  describe("Movie search") {
    it("adds two imovies") {
      persistenceManager.saveIMovie(firstIMovie)
      persistenceManager.saveIMovie(secondIMovie)
      persistenceManager.saveIMovie(thirdIMovie)
      persistenceManager.listIMovies().size should be(3)
    }

    it("indexes and finds a movie") {
      esi.indexIMovie(imovieIndexName, firstIMovie, flush = true)
      esi.indexIMovie(imovieIndexName, secondIMovie, flush = true)
      ms.searchTitle("Movie One") should be(Some(firstMovie))
      ms.searchTitle("Movie Two") should be(Some(secondMovie))
    }

    it("indexes and finds a series") {
      esi.indexIMovie(imovieIndexName, thirdIMovie, flush = true)
      ms.searchTitle("Series One") should be(Some(firstSeries))
    }
    
    it("cleans up") {
      esi.deleteIndex(imovieIndexName)
    }
  }
}
