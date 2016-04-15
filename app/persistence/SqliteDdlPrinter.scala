package persistence

import scala.slick.driver.SQLiteDriver

object SqliteDdlPrinter {
  val driver = SQLiteDriver

  val dals = Seq(new LearnBySubtitlesDAL(driver))

  def main(args: Array[String]) {
    dals.foreach { d =>
      println(d.createStatements.mkString("\n"))
    }
  }
}
