package search

import persistence.NoopPersistenceManager
import subtitles.OpenSubtitlesSearcher

object SearchSubtitleTask {
  def main(args: Array[String]) {
    if (args.length != 1) {
      println("We currently support only a single imdbid")
      return
    }
    val searcher = new OpenSubtitlesSearcher(new NoopPersistenceManager)
    val result = searcher.getSubtitleCandidates(args(0))
    result.foreach(println)
  }
}
