package search

import logging.Logging
import persistence.PersistenceManager
import subtitles.SubtitleSearcher

class SubtitleIndexingTask(persistenceManager: PersistenceManager,
                           subtitleSearcher: SubtitleSearcher,
                           searchInteractor:  SearchInteractor,
                           index: String) extends Runnable with Logging {
  def run() {
    val toBeIndexed = persistenceManager.findSubtitlesToIndex()
    if (toBeIndexed.nonEmpty) {
      logger.info(s"Files to be indexed: $toBeIndexed")
    }
    toBeIndexed.foreach { s =>
      val subtitleText = subtitleSearcher.getSubtitleContent(s)
      searchInteractor.indexSubtitleContent(index, subtitleText, s.id, s.imdbId, flush = false)
      persistenceManager.markSubtitleAsIndexed(s.id)
    }
    searchInteractor.flushIndex(index)
  }
}
