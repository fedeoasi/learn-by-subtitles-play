package omdb

import scala.util.Try
import java.text.NumberFormat
import java.util.Locale
import scala.io.Source
import OmdbDumpFields._

object OmdbDumpUtils {
  val tabRegex = "\\t"
  val numberFormatter = NumberFormat.getNumberInstance(Locale.getDefault)

  def extractInterestingFieldIndex(indexByField: Map[String, Int]): List[Int] = {
    interestingFields.map { f => indexByField(f) }
  }

  def extractHeader(inputLines: Iterator[String]): Seq[String] = {
    val header = inputLines.next()
    header.split(tabRegex).toSeq
  }

  def extractHeaderIndex(inputLines: Iterator[String]): Map[String, Int] = {
    extractHeader(inputLines).zipWithIndex.toMap
  }

  def parseOptionalInt(s: String): Option[Int] = Try {
    numberFormatter.parse(s).intValue()
  }.toOption

  def parseOptionalDouble(s: String): Option[Double] = Try {
    numberFormatter.parse(s).doubleValue()
  }.toOption

  def dumpInputStreamLines(location: String): Iterator[String] = {
    Source.fromFile(location, "iso-8859-1").getLines()
  }
}
