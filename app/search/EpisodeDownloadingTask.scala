package search

import com.google.inject.Inject
import config.Config
import logging.Logging
import persistence.PersistenceManager
import services.SubtitlesService

import scala.util.{ Failure, Random, Success, Try }

class EpisodeDownloadingTask @Inject() (
    persistenceManager: PersistenceManager,
    subtitlesController: SubtitlesService
) extends Runnable
    with Logging {

  val batchSize = 100

  def run() {
    Try {
      val toBeIndexed =
        persistenceManager.findEpisodesWithNoSubtitles().take(batchSize)
      logger.info(s"Files to download subtitles for: $toBeIndexed")
      Random.shuffle(toBeIndexed).foreach { s =>
        logger
          .info("Downloading subtitles for episode with imdbId: " + s.imdbID)
        subtitlesController.getSubtitlesForMovie(s.imdbID)
        Thread.sleep(Config.millisBetweenDownloads)
      }
    } match {
      case Failure(e) =>
        logger.error("Could not retrieve subtitles for episode batch", e)
      case Success(_) =>
    }
  }
}
