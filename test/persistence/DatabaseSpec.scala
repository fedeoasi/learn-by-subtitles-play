package persistence

import model._
import org.joda.time.DateTime
import org.scalatest.{OptionValues, FunSpec, Matchers}

trait WithPersistenceManager {
  val persistenceManager: PersistenceManager = new TestPersistenceManager()
}

class DatabaseSpec extends FunSpec with Matchers with WithPersistenceManager with OptionValues {
  val movie =  Movie("imdbId", 2013, "testMovie", "http://something.jpg", None)

  describe("Movies") {
    it("return None for a non existent movie") {
      val movie = persistenceManager.findMovieById("randomId")
      movie should be(None)
    }

    it("should be able to save a movie") {
      persistenceManager.saveMovie(movie)
      val returnedMovie = persistenceManager.findMovieById(movie.imdbID)
      returnedMovie.value.copy(id = None) should be(movie)
    }

    it("should not error out when adding an existing movie") {
      persistenceManager.saveMovie(movie)
      val returnedMovie = persistenceManager.findMovieById(movie.imdbID)
      returnedMovie.value.copy(id = None) should be(movie)
    }

    it("should delete a movie") {
      val movie =  Movie("toDelete", 2013, "testMovie", "http://something.jpg", None)
      persistenceManager.saveMovie(movie)
      val returnedMovie = persistenceManager.findMovieById(movie.imdbID)
      returnedMovie.value.copy(id = None) should be(movie)
      persistenceManager.deleteMovie(movie.imdbID)
      persistenceManager.findMovieById(movie.imdbID) should be(None)
    }
  }

  describe("Subtitles") {
    it("return None for a non-existing subtitle") {
      val movie = persistenceManager.findSubtitleForMovie("randomId")
      movie should be(None)
    }

    it("should be able to save a subtitle") {
      val movie =  Movie("testMovie", 2012, "testMovie", "http://something.jpg", None)
      persistenceManager.saveMovie(movie)
      val subtitle = Subtitle("abc", "testMovie")
      persistenceManager.saveSubtitle(subtitle)
      val returnedMovie = persistenceManager.findSubtitleForMovie(subtitle.imdbId)
      returnedMovie should be(Some(subtitle))
    }

    it("should not error out when adding an existing movie") {
      val subtitle = Subtitle("abc", "testMovie")
      persistenceManager.saveSubtitle(subtitle)
      val returnedMovie = persistenceManager.findSubtitleForMovie(subtitle.imdbId)
      returnedMovie should be(Some(subtitle))
    }

    it("should not error out when trying to add an existing subtitle") {
      val subtitle = Subtitle("abc", "testMovie2")
      persistenceManager.saveSubtitle(subtitle)
      val returnedMovie = persistenceManager.findSubtitleById(subtitle.id)
      returnedMovie should be(Some(Subtitle("abc", "testMovie")))
    }

    it("should list the inserted subtitles as non-indexed") {
      val movie =  Movie("testMovie3", 2013, "testMovie3", "http://something3.jpg", None)
      persistenceManager.saveMovie(movie)
      val subtitle = Subtitle("abcd", "testMovie3")
      persistenceManager.saveSubtitle(subtitle)
      val subtitles = persistenceManager.findSubtitlesToIndex()
      subtitles.size should be(2)
    }

    it("should list mark a subtitle as indexed") {
      val movie =  Movie("testMovie4", 2013, "testMovie4", "http://something4.jpg", None)
      persistenceManager.saveMovie(movie)
      val id: String = "abcd"
      val subtitle = Subtitle(id, "testMovie4")
      persistenceManager.saveSubtitle(subtitle)
      persistenceManager.markSubtitleAsIndexed(id)
      val subtitles = persistenceManager.findSubtitlesToIndex()
      subtitles.size should be(1)
    }
  }

  describe("Series") {
    val series =  Series("sImdbId", 1, "How I Met You", "http://something", None)

    it("return None for a non existent series") {
      val returnedSeries = persistenceManager.findSeriesById("randomId")
      returnedSeries should be(None)
    }

    it("should be able to save and retrieve a series") {
      persistenceManager.saveSeries(series)
      val returnedSeries = persistenceManager.findSeriesById(series.imdbID)
      returnedSeries.value.copy(id = None) should be(series)
      val returnedMovie = persistenceManager.findMovieById(series.imdbID)
      returnedMovie should be(None)
    }

    it("should not retrieve a movie with the given id") {
      val movie =  Movie("msImdbId", 1, "Green Pile", "http://something", None)
      persistenceManager.saveMovie(movie)
      val returnedMovie = persistenceManager.findSeriesById(movie.imdbID)
      returnedMovie should be(None)
    }

  }

  describe("Episodes") {
    it("return None for a non existent subtitle") {
      val episode = persistenceManager.findEpisodeById("randomId")
      episode should be(None)
    }

    it("should be able to save an episode") {
      val episode =  Episode("eimdbId", 1, 1, "seriesId")
      persistenceManager.saveEpisode(episode)
      val returnedMovie = persistenceManager.findEpisodeById(episode.imdbID)
      returnedMovie should be(Some(episode))
    }

    it("should not error out when adding an existing movie") {
      val episode =  Episode("eimdbId", 1, 1, "seriesId")
      persistenceManager.saveEpisode(episode)
      val returnedMovie = persistenceManager.findEpisodeById(episode.imdbID)
      returnedMovie should be(Some(episode))
    }

    it("should find an episode by series id, season, and episode number") {
      val episode =  Episode("eimdbId", 1, 1, "seriesId")
      persistenceManager.saveEpisode(episode)
      val series = Series("seriesId", 2000, "my series", "http://blabla", None)
      val returnedMovie = persistenceManager.findEpisodeForSeries(series.imdbID, 1, 1)
      returnedMovie should be(Some(episode))
    }

    it("should list the episodes with no subtitles") {
      val episode1 =  Episode("eimdbId", 1, 1, "seriesId")
      persistenceManager.saveEpisode(episode1)
      val episode2 =  Episode("eimdbId2", 1, 2, "seriesId")
      persistenceManager.saveEpisode(episode2)
      val episode3 =  Episode("eimdbId3", 1, 3, "seriesId")
      persistenceManager.saveEpisode(episode3)

      persistenceManager.saveSubtitle(Subtitle("idForListingEpisodes", "eimdbId2"))
      val episodesToProcess: List[Episode] = persistenceManager.findEpisodesWithNoSubtitles()
      episodesToProcess.size should be(2)
      episodesToProcess should contain(episode1)
      episodesToProcess should contain(episode3)
    }

    describe("Movies and Episodes") {
      it("should return a map imdbId -> title for the requested imdbIds") {
        val movie = Movie("meId", 2013, "testMovie", "http://something.jpg", None)
        val series = Series("meId2", 2010, "testSeries", "http://something.jpg", None)
        persistenceManager.saveMovie(movie)
        persistenceManager.saveSeries(series)
        val episode =  Episode("meId3", 1, 1, "meId2")
        persistenceManager.saveEpisode(episode)
        val map = persistenceManager.titlesByImdbId(Seq("meId", "meId3"))
        map.size should be(2)
        map("meId") should be("testMovie")
        map("meId3") should be("testSeries s1e1")
      }
    }

    describe("Subtitle downloads") {
      it("should add a subtitle download") {
        val time = DateTime.now
        persistenceManager.subtitleDownloadsSince(time) should be(0)
        persistenceManager.saveSubtitleDownload()
        persistenceManager.subtitleDownloadsSince(time) should be(1)
      }

      it("does not find any download errors by imdbId") {
        persistenceManager.downloadErrorsFor("imdbId") shouldBe Seq.empty
      }

      it("does not find any download errors") {
        persistenceManager.lastKDownloadErrors(2) shouldBe Seq.empty
      }

      it("should add and find a download error") {
        persistenceManager.saveDownloadError("sId", "imdbId", "Can't find file")
        persistenceManager.downloadErrorsFor("imdbId").size shouldBe 1
        persistenceManager.downloadErrorsFor("imdbId2").size shouldBe 0
      }

      it("should find a download error") {
        val lastKDownloadErrors = persistenceManager.lastKDownloadErrors(2)
        lastKDownloadErrors.size shouldBe 1
      }

      it("should find two download errors") {
        persistenceManager.saveDownloadError("sId2", "imdbId", "Can't find file")
        persistenceManager.lastKDownloadErrors(2).size shouldBe 2
      }

      it("should find the two latest download errors") {
        persistenceManager.saveDownloadError("sId3", "imdbId", "Can't find file")
        persistenceManager.lastKDownloadErrors(2).map(_.subtitleId) shouldBe Seq("sId3", "sId2")
      }
    }
  }
}
