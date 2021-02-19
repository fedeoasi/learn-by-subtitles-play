package parsing

import java.io.{ByteArrayInputStream, InputStream}
import java.text.SimpleDateFormat
import java.util.Scanner
import java.util.regex.Pattern

import com.google.inject.Inject
import model.SubEntry

import logging.Logging

class SrtParser @Inject() () extends Logging {
  private val timePattern = Pattern.compile("\\d\\d:\\d\\d:\\d\\d,\\d\\d\\d")
  val timeFormat = new SimpleDateFormat("HH:mm:ss,SSS")

  def parseSrt(input: String): List[SubEntry] = {
    parseSrt(input, onlyHighlighted = false)
  }

  def parseSrt(input: String, onlyHighlighted: Boolean): List[SubEntry] = {
    val delimiter = getDelimiter(input)
    parseSrt(getInputStream(input), onlyHighlighted, delimiter)
  }

  def parseWithinHighlighted(input: String, offset: Int): List[SubEntry] = {
    val totalEntries = countEntries(input)
    val highlighted = highlightedLines(input)
    val intervals = computeIntervals(highlighted, offset, totalEntries)
    parseSrt(getInputStream(input), onlyHighlighted = false, getDelimiter(input),
      index => intervals.exists(i => index >= i._1 && index <= i._2))
  }

  private[parsing] def computeIntervals(indices: List[Int], offset: Int, end: Int): List[(Int, Int)] = {
    val emptyIntervals = List.empty[(Int, Int)]
    indices.foldLeft((emptyIntervals, 0)) {
      case ((intervals, previousEnd), index) =>
        val s = Math.max(previousEnd + 1, index - offset)
        val e = Math.min(end, index + offset)
        if(s > e) {
          (intervals, previousEnd)
        } else {
          (intervals :+ (s, e), e)
        }
    }._1
  }

  private[parsing] def highlightedLines(input: String): List[Int] = {
    parseSrt(getInputStream(input), onlyHighlighted = true, getDelimiter(input)).map {
      se => se.number
    }
  }

  private[parsing] def countEntries(input: String): Int = {
    countEntries(getInputStream(input), getDelimiter(input))
  }

  private def parseSrt(input: InputStream,
                       onlyHighlighted: Boolean,
                       delimiter: String,
                       filterByIndex: (Int) => Boolean = _ => true): List[SubEntry] = {
    foldSrt(input, delimiter, (List.empty[SubEntry], 1)) {
      case ((list, index), line) =>
        if(filterByIndex(index)) {
          readEntry(index, line.replaceAll("\r", ""), onlyHighlighted) match {
            case Some(x) => (x :: list, index + 1)
            case None => (list, index + 1)
          }
        } else {
          (list, index + 1)
        }
    }._1.reverse
  }


  private def countEntries(input: InputStream, delimiter: String): Int = {
    foldSrt(input, delimiter, 0) {
      case (acc, line) =>
        if(line.matches("^[\r\n]+$")) {
          acc
        } else {
          acc + 1
        }
    }
  }

  private def foldSrt[T](input: InputStream, delimiter: String, initialValue: T)
                        (f: (T, String) => T): T = {
    val sc = new Scanner(input)
    sc.useDelimiter(delimiter)
    var result = initialValue
    while (sc.hasNext) {
      result = f(result, sc.next())
    }
    sc.close()
    result
  }

  private def readEntry(num: Int, string: String, onlyHighlighted: Boolean): Option[SubEntry] = {
    try {
      if(string.matches("^[\r\n]+$")) {
        None
      } else {
        val sc = new Scanner(string)
        sc.nextLine
        val start = timeFormat.parse(sc.findWithinHorizon(timePattern, 0))
        val end = timeFormat.parse(sc.findWithinHorizon(timePattern, 0))
        sc.useDelimiter("\\Z")
        val text = sc.next()
        if(onlyHighlighted && !text.contains("<em>")) {
          None
        } else {
          Some(SubEntry(num, start, end, text))
        }
      }
    } catch {
      case t: Throwable =>
        logger.error("Exception while processing string: " + string + " " + t.printStackTrace())
        None
    }
  }

  private def getDelimiter(input: String): String = {
    if(input.contains("\r\n")) {
      "\r\n\r"
    } else {
      "\n\n"
    }
  }

  private def getInputStream(input: String): ByteArrayInputStream = {
    new ByteArrayInputStream(input.getBytes)
  }
}
