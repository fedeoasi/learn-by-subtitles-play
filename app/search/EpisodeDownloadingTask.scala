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
    logger.info("Finding episodes to download")
    val toBeIndexed = persistenceManager.findEpisodesWithNoSubtitles().take(batchSize)
    logger.info(s"Found ${toBeIndexed.size} episodes to download subtitles for")
    logger.info(s"Files to be downloaded: $toBeIndexed")
    toBeIndexed.foreach { s =>
      logger.info("Downloading subtitles for episode with imdbId: " + s.imdbID)
      subtitlesController.getSubtitlesForMovie(s.imdbID)
      Thread.sleep(Config.millisBetweenDownloads)
    }
  }
}