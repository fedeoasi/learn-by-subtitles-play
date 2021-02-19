import java.util.concurrent.{Executors, ScheduledExecutorService, TimeUnit}

import com.github.ghik.silencer.silent
import config.Config
import logging.Logging
import persistence.PersistenceManager
import play.GlobalSettings
import search.{EpisodeDownloadingTask, SearchInteractor, SubtitleIndexingTask}
import subtitles.SubtitleSearcher

@silent("deprecated")
class Global extends GlobalSettings with Logging {
  private var indexerPool: ScheduledExecutorService = _
  private var downloaderPool: ScheduledExecutorService = _

  override def onStart(app: play.Application): Unit = {
    val persistenceManager = app.injector.instanceOf(classOf[PersistenceManager])
    val searchInteractor = app.injector.instanceOf(classOf[SearchInteractor])
    val subtitleSearcher = app.injector.instanceOf(classOf[SubtitleSearcher])
    val indexName = Config.indexName
    try {
      searchInteractor.ensureIndexExists(indexName)
    } catch {
      case e: Throwable => logger.error("Unable to ensure index existence:\n" + e.getStackTrace.mkString("\n"))
    }
    val subtitleIndexingTask = new SubtitleIndexingTask(persistenceManager, subtitleSearcher, searchInteractor, indexName)
    indexerPool = Executors.newScheduledThreadPool(1)
    indexerPool.scheduleAtFixedRate(subtitleIndexingTask, 0, 5, TimeUnit.SECONDS)
    if(Config.episodeDownloadingEnabled) {
      val episodesDownloadingTask = app.injector.instanceOf(classOf[EpisodeDownloadingTask])
      downloaderPool = Executors.newScheduledThreadPool(1)
      downloaderPool.scheduleAtFixedRate(episodesDownloadingTask, 0, 30, TimeUnit.SECONDS)
    }
    super.onStart(app)
  }

  override def onStop(app: play.Application): Unit = {
    if(indexerPool != null) {
      indexerPool.shutdown()
    }
    if(downloaderPool != null) {
      downloaderPool.shutdownNow()
    }
    super.onStop(app)
  }
}
