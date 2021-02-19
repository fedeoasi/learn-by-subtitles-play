package controllers

import com.google.inject.Inject
import org.joda.time.DateTime
import org.json4s.DefaultFormats
import org.json4s.jackson.Serialization._
import persistence.PersistenceManager
import play.api.mvc.InjectedController
import subtitles.SubtitleSearcher

case class Stats(movieCount: Int,
                 seriesCount: Int,
                 subtitlesToIndexCount: Int,
                 episodesWithNoSubtitlesCount: Int,
                 subtitleCount: Int,
                 nextAvailableDownload: DateTime)

class StatsController @Inject()(persistenceManager: PersistenceManager,
                                subtitleSearcher: SubtitleSearcher) extends InjectedController {
  implicit val formats = DefaultFormats ++ org.json4s.ext.JodaTimeSerializers.all

  def stats = Action {
    val movieCount = persistenceManager.listMovies().size
    val seriesCount = persistenceManager.listSeries().size
    val subtitlesToIndexCount = persistenceManager.findSubtitlesToIndex().size
    val episodesWithNoSubtitlesCount = persistenceManager.findEpisodesWithNoSubtitles().size
    val subtitleCount = persistenceManager.listSubtitles().size
    val nextAvailableDownload = subtitleSearcher.nextAvailableDownload
    val latestStats = Stats(
      movieCount,
      seriesCount,
      subtitlesToIndexCount,
      episodesWithNoSubtitlesCount,
      subtitleCount,
      nextAvailableDownload)
    Ok(writePretty(latestStats))
  }
}
