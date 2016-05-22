package search

import com.google.inject.Inject
import config.Config
import logging.Logging
import persistence.PersistenceManager
import services.SubtitlesService

class EpisodeDownloadingTask @Inject() (persistenceManager: PersistenceManager,
                                        subtitlesController: SubtitlesService)
  extends Runnable with Logging {

  val batchSize = 10

  def run() {
    val toBeIndexed = persistenceManager.findEpisodesWithNoSubtitles().take(batchSize)
    logger.info(s"Files to download subtitles for: $toBeIndexed")
    toBeIndexed.foreach { s =>
      logger.info("Downloading subtitles for episode with imdbId: " + s.imdbID)
      subtitlesController.getSubtitlesForMovie(s.imdbID)
      Thread.sleep(Config.millisBetweenDownloads)
    }
  }
}