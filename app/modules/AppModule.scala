package modules

import com.google.inject.AbstractModule
import logging.Logging
import persistence.{ProdPersistenceManager, PersistenceManager}
import play.api.{Configuration, Environment}
import search.{ElasticSearchInteractor, SearchInteractor}
import subtitles.{OpenSubtitlesSearcher, SubtitleSearcher}

class AppModule(env: Environment, config: Configuration) extends AbstractModule with Logging {
  override def configure(): Unit = {
    bind(classOf[SearchInteractor]).to(classOf[ElasticSearchInteractor])
    bind(classOf[PersistenceManager]).toInstance(ProdPersistenceManager())
    bind(classOf[SubtitleSearcher]).to(classOf[OpenSubtitlesSearcher])
  }
}
