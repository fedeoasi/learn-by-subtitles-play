package search

import java.text.SimpleDateFormat
import java.util.UUID

import logging.Logging
import model.SubEntry
import org.scalatest.{ FunSpec, Matchers }
import parsing.SrtParser
import utils.Utils

import scala.io.Source._

class ElasticSearchSpec extends FunSpec with Matchers with Logging {
  val format = new SimpleDateFormat("HH:mm:ss,SSS")

  describe("Elastic Search") {
    describe("Index creation and deletion") {
      it("should be able to ensure the existence of an index") {
        val interactor = new ElasticSearchInteractor()
        interactor.ensureIndexExists("test-index") should be(right = true)
        interactor.close()
      }

      it("should be able to delete an index") {
        val interactor = new ElasticSearchInteractor()
        val name = "test-index"
        interactor.ensureIndexExists(name) should be(right = true)
        interactor.deleteIndex(name)
        interactor.close()
      }
    }

    describe("Subtitle indexing") {
      it("should be able to index an entry of a subtitle file") {
        withInteractorAndIndex { case (interactor, name) =>
          val entry = SubEntry(
            1,
            format.parse("00:15:11,123"),
            format.parse("00:16:11,123"),
            "This is the content"
          )
          interactor.indexSubtitleEntry(
            name,
            entry,
            "subId",
            "imdbId",
            flush = true
          )
          val results = interactor.searchSubtitleEntries(name, "content")
          results.size should be(1)
        }
      }

      it("should be able to index an entire subtitle file entry by entry") {
        withInteractorAndIndex { case (interactor, name) =>
          val input =
            fromFile("resources/four-entries.srt").getLines().mkString("\n")
          val entries = new SrtParser().parseSrt(input)
          interactor.indexSubtitleEntries(name, entries, "subId", "imdbId")
          val results = interactor.searchSubtitleEntries(name, "line")
          results.size should be(3)
        }
      }

      it("should be able to index an entire subtitle file") {
        withInteractorAndIndex { case (interactor, name) =>
          val fileString =
            Utils.readFileIntoString("resources/four-entries.srt")
          interactor.indexSubtitleContent(
            name,
            fileString,
            "subId",
            "imdbId",
            flush = true
          )
          val results = interactor.searchSubtitles(name, "line")
          results.size should be(1)
          results.head.subtitleId should be("subId")
          results.head.movieId should be("imdbId")
        }
      }

      it("should be able to index and search two subtitle files") {
        withInteractorAndIndex { case (interactor, name) =>
          val file1String =
            Utils.readFileIntoString("resources/four-entries.srt")
          val file2String = Utils.readFileIntoString("resources/other.srt")
          interactor.indexSubtitleContent(
            name,
            file1String,
            "subId1",
            "imdbId1",
            flush = true
          )
          interactor.indexSubtitleContent(
            name,
            file2String,
            "subId2",
            "imdbId2",
            flush = true
          )
          val results1 = interactor.searchSubtitles(name, "line")
          results1.size should be(1)
          results1.head.subtitleId should be("subId1")
          results1.head.movieId should be("imdbId1")
          val results2 = interactor.searchSubtitles(name, "third")
          results2.size should be(2)
        }
      }

      it("should allow the same subtitle to be indexed twice") {
        withInteractorAndIndex { case (interactor, name) =>
          val file1String =
            Utils.readFileIntoString("resources/four-entries.srt")
          val file2String = Utils.readFileIntoString("resources/other.srt")
          interactor.indexSubtitleContent(
            name,
            file1String,
            "subId1",
            "imdbId1",
            flush = true
          )
          interactor.indexSubtitleContent(
            name,
            file2String,
            "subId2",
            "imdbId1",
            flush = true
          )
          val entries = interactor.findEntriesForMovie(name, "imdbId1")
          entries.size should be(2)
        }
      }
    }
  }

  describe("Auto-completion") {
    it("should suggest names of movies") {
      withInteractorAndIndex { case (interactor, name) =>
        interactor.indexMovie(name, "1", "How I Met Your Mother", flush = false)
        interactor.indexMovie(
          name,
          "2",
          "How to get away with murder",
          flush = false
        )
        interactor.indexMovie(name, "3", "Murder, She Wrote", flush = true)
        interactor.indexMovie(name, "4", "Mountain hike", flush = true)
        interactor.suggestMovie(name, "How").size should be(2)
        interactor.suggestMovie(name, "M").size should be(2)
        interactor.suggestMovie(name, "Mu").size should be(1)
        interactor.suggestMovie(name, "Mo").size should be(1)
      }
    }
  }

  def withInteractorAndIndex(
    test: (ElasticSearchInteractor, String) => Unit
  ): Unit = {
    val interactor = new ElasticSearchInteractor()
    val index = "test-index" + UUID.randomUUID()
    logger.info("Cluster name: " + index)
    val success = interactor.ensureMovieIndex(index)
    success should be(right = true)
    test(interactor, index)
    interactor.deleteIndex(index)
    interactor.close()
  }
}
