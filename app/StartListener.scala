import java.util.concurrent.{ Executors, TimeUnit }

import com.google.inject.Inject
import config.Config
import logging.Logging
import persistence.PersistenceManager
import play.api.inject.ApplicationLifecycle
import search.{ EpisodeDownloadingTask, SearchInteractor, SubtitleIndexingTask }
import subtitles.SubtitleSearcher

import scala.concurrent.Future

trait StartListener

class StartListenerImpl @Inject() (
    persistenceManager: PersistenceManager,
    searchInteractor: SearchInteractor,
    subtitleSearcher: SubtitleSearcher,
    episodesDownloadingTask: EpisodeDownloadingTask,
    applicationLifecycle: ApplicationLifecycle
) extends StartListener
    with Logging {

  init()

  def init(): Unit = {
    logger.info("Invoking start listener.")
    val indexName = Config.indexName
    try {
      searchInteractor.ensureIndexExists(indexName)
    } catch {
      case e: Throwable =>
        logger.error(
          "Unable to ensure index existence:\n" + e.getStackTrace.mkString("\n")
        )
    }
    val subtitleIndexingTask = new SubtitleIndexingTask(
      persistenceManager,
      subtitleSearcher,
      searchInteractor,
      indexName
    )
    val indexerPool = Executors.newScheduledThreadPool(1)
    indexerPool.scheduleAtFixedRate(
      subtitleIndexingTask,
      0,
      5,
      TimeUnit.SECONDS
    )

    applicationLifecycle.addStopHook(() =>
      Future.successful {
        logger.info("Stopping the indexer thread pool")
        indexerPool.shutdown()
      }
    )

    if (Config.episodeDownloadingEnabled) {
      val downloaderPool = Executors.newScheduledThreadPool(1)
      downloaderPool.scheduleAtFixedRate(
        episodesDownloadingTask,
        0,
        30,
        TimeUnit.SECONDS
      )
      applicationLifecycle.addStopHook(() =>
        Future.successful {
          logger.info("Stopping the episode downloader thread pool")
          downloaderPool.shutdown()
        }
      )
    }
  }

}
