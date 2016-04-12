package parsing

import java.io.{File, FileInputStream, StringWriter}

import org.apache.commons.io.IOUtils
import org.scalatest.{FunSpec, Matchers}
import scala.io.Source._

class SrtParsingSpec extends FunSpec with Matchers {
  val parser = new SrtParser()

  def loadFileIntoString(filename: String): String = {
    val writer = new StringWriter()
    IOUtils.copy(new FileInputStream(filename), writer, "UTF-8")
    writer.toString
  }

  describe("Parsing Srt") {
    it("should load a text file") {
      val source = fromFile(new File("resources/abc.txt"))
      source.mkString should be("abcd")
    }

    it("should load an srt file into a list of entries -- Die Hard") {
      val input = loadFileIntoString("resources/4899518.srt")
      val entries = parser.parseSrt(input)
      entries.size should be(813)
      entries.head.text should be("\nWaft your waves, ye waters!\nCarry your crests to the cradle!")
      entries.head.number should be(1)
      entries(812).number should be(813)
      entries(812).text should be("\nFalse and faint-hearted\nare those who revel above!")
      parser.countEntries(input) should be(813)
    }

    it("should load an srt file into a list of entries -- HYMYM") {
      val input = loadFileIntoString("resources/3155806.srt")
      val entries = parser.parseSrt(input)
      entries.size should be(563)
      entries.head.text should be("\nit was saturday night in new york city,")
      entries.head.number should be(1)
      entries(562).number should be(563)
      entries(562).text should be("\noh, and it's gonna be muddy.")
      parser.countEntries(input) should be(563)
    }


    it("should load another srt file into a list of entries") {
      val input = loadFileIntoString("resources/four-entries.srt")
      val entries = parser.parseSrt(input)
      entries.size should be(4)
      entries.head.text should be("\nHello, this is the first line")
      entries.head.number should be(1)
      entries(3).number should be(4)
      entries(3).text should be("\nEnd!")
      parser.countEntries(input) should be(4)
    }

    it("should filter out the non-highlighted lines") {
      val input = loadFileIntoString("resources/highlighted.srt")
      val entries = parser.parseSrt(input, onlyHighlighted = true)
      entries.size should be(7)
      entries.head.text should be("\nYeah, hi, it's Bill <em>Lumbergh</em>.")
      entries.head.number should be(498)
      entries(6).number should be(1374)
      entries(6).text should be("\n<em>Lumbergh</em>?")
    }

    it("should compute the highlighted lines") {
      val input = loadFileIntoString("resources/highlighted.srt")
      val highlightedLines = parser.highlightedLines(input)
      highlightedLines should be(
        List(498, 505, 743, 768, 948, 1372, 1374)
      )
    }

    it("should compute the appropriate intervals for offset 1") {
      val lines = List(498, 505, 743, 768, 948, 1372, 1374)
      val intervals = parser.computeIntervals(lines, 1, 1374)
      intervals should be(
        List((497, 499), (504, 506), (742, 744), (767, 769), (947, 949), (1371, 1373), (1374, 1374))
      )
    }

    it("should compute the appropriate intervals for offset 10") {
      val lines = List(498, 505, 743, 768, 948, 1372, 1374)
      val intervals = parser.computeIntervals(lines, 10, 1374)
      intervals should be(
        List((488, 508), (509, 515), (733, 753), (758, 778), (938, 958), (1362, 1374))
      )
    }

    it("should return the appropriate lines for offset 1") {
      val input = loadFileIntoString("resources/highlighted.srt")
      val lines = parser.parseWithinHighlighted(input, 1)
      lines.size should be(19)
      lines.head.number should be(497)
      lines.head.text should be("\n[Beep]")
      lines(18).number should be(1374)
      lines(18).text should be("\n<em>Lumbergh</em>?")
    }

    it("should return the appropriate lines for offset 10") {
      val input = loadFileIntoString("resources/highlighted.srt")
      val lines = parser.parseWithinHighlighted(input, 10)
      lines.size should be(104)
      lines.head.number should be(488)
      lines.head.text should be("\nANNE: Oh! Where's the phone?\nWhere's the goddamn phone?!")
      lines(103).number should be(1374)
      lines(103).text should be("\n<em>Lumbergh</em>?")
    }

//    it("should parse every subtitle in the subtitleFiles folder") {
//      val files = new File("subtitleFiles").listFiles()
//      var cannotParse = List.empty[String]
//      files.toList.filter(_.getName.endsWith(".srt")).foreach { f =>
//        val srt = loadFileIntoString(f.getAbsolutePath)
//        val parsed = parser.parseSrt(srt)
//        if (parsed == null || parsed.size < 5) {
//          cannotParse = f.getName :: cannotParse
//        }
//      }
//      println(s"Couldn't parse: $cannotParse")
//    }
  }
}
