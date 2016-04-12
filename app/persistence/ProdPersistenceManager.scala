package persistence

import logging.Logging
import play.api.Logger

import scala.slick.driver.SQLiteDriver
import scala.slick.driver.SQLiteDriver.simple._

class ProdPersistenceManager extends BasePersistenceManager with Logging {
  val database = Database.forURL("jdbc:sqlite:%s.db" format "lbs",
    driver = "org.sqlite.JDBC")
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
