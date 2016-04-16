package persistence

import logging.Logging
import play.api.Logger

import scala.slick.driver.SQLiteDriver

class ProdPersistenceManager extends BasePersistenceManager with Logging {
  val database = SQLiteDatabaseInitializer.database("lbs")
  override val dal: LearnBySubtitlesDAL = new LearnBySubtitlesDAL(SQLiteDriver)
  initializeDatabase()
}

object ProdPersistenceManager{
  lazy val INSTANCE = new ProdPersistenceManager()
  def apply() = {
    Logger(classOf[ProdPersistenceManager]).info("Initializing Production database")
    INSTANCE
  }
}
