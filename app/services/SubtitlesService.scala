package services

import com.google.inject.Inject
import model.SubEntry
import parsing.SrtParser
import persistence.PersistenceManager
import subtitles.SubtitleSearcher

class SubtitlesService @Inject() (persistenceManager: PersistenceManager,
                                  srtParser: SrtParser,
                                  searcher: SubtitleSearcher) {

  def getSubtitlesForMovie(imdbId: String): List[SubEntry] = {
    var subtitles = List[SubEntry]()
    val subString = searcher.searchSubtitles(imdbId)
    if (subString != null) {
      subtitles = srtParser.parseSrt(subString)
    }
    subtitles
  }
}
