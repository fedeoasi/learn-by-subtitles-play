package persistence

import java.util.UUID

import scala.slick.driver.H2Driver
import scala.slick.driver.H2Driver.simple._

class TestPersistenceManager extends BasePersistenceManager {
  val database = Database.forURL(s"jdbc:h2:mem:lbsTest" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1",
    driver = "org.h2.Driver")
  override val dal: LearnBySubtitlesDAL = new LearnBySubtitlesDAL(H2Driver)

  initializeDatabase()
}