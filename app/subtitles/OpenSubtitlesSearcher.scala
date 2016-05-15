package subtitles

import com.google.inject.Inject
import logging.Logging
import model._
import org.apache.xmlrpc.client.XmlRpcClient
import org.apache.xmlrpc.client.XmlRpcClientConfigImpl
import org.apache.xmlrpc.client.XmlRpcSunHttpTransportFactory
import java.net.URL
import scala.collection.JavaConversions._
import model.Subtitle
import scala.collection.{immutable, mutable}
import org.joda.time.DateTime
import java.util
import config.Config
import java.lang
import persistence.PersistenceManager

import scala.util.control.NonFatal

trait SubtitleSearcher {
  def searchSubtitles(imdbId: String): String
  def downloadSubtitle(id: String): Option[String]
  def getSubtitleContent(existingSubtitle: Subtitle): String
  def searchSubtitlesOnline(imdbId: String): Option[SubtitleWithContent]
  def getSubtitleCandidates(imdbId: String): Array[Object]
}

trait SeriesDetailProvider {
  def get(imdbId: String): List[Season]
}

class OpenSubtitlesSearcher @Inject() (persistenceManager: PersistenceManager)
  extends SubtitleSearcher with SeriesDetailProvider with Logging {

  val APP_USER_AGENT = "LBS_USER_AGENT"
  val HOURS_PER_DOWNLOAD_PERIOD: Int = 12
  val MAX_DOWNLOADS_PER_PERIOD: Int = 100

  val config = {
    val cfg = new XmlRpcClientConfigImpl()
    cfg.setServerURL(new URL("http://api.opensubtitles.org:80/xml-rpc"))
    cfg.setEncoding("ISO-8859-1")
    cfg.setConnectionTimeout(Config.connectionTimeout)
    cfg
  }
  val client = {
    val cli = new XmlRpcClient()
    cli.setTransportFactory(new XmlRpcSunHttpTransportFactory(cli))
    cli.setConfig(config)
    cli
  }
  val subtitleStorage = new LocalSubtitleStorage

  var token = ""

  def login() = {
    val params = Array[AnyRef]("", "", "", System.getenv(APP_USER_AGENT))
    val response = client.execute(config, "LogIn", params)
    asJavaStringMap(response).get("token")
  }

  def checkToken(token: String): Boolean = {
    if(token.isEmpty) false else {
      val response = client.execute(config, "NoOperation", Array[AnyRef](token))
      val status = asJavaStringMap(response).get("status")
      status != null && status.contains("200")
    }
  }

  override def searchSubtitles(imdbId: String): String = {
    persistenceManager.findSubtitleForMovie(imdbId) match {
      case Some(existingSubtitle) => getSubtitleContent(existingSubtitle)
      case None =>
        searchSubtitlesOnline(imdbId) match {
          case Some(SubtitleWithContent(subtitle, content)) =>
            subtitleStorage.persistSubtitleFile(subtitle, content)
            persistenceManager.saveSubtitle(subtitle)
            content
          case None => null
        }
    }
  }

  override def getSubtitleContent(existingSubtitle: Subtitle): String =
    subtitleStorage.retrieveSubtitleContent(existingSubtitle)

  override def getSubtitleCandidates(imdbId: String): Array[Object] = {
    val cleanId = cleanImdbId(imdbId)
    logger.debug(s"searching for $cleanId")
    val searchParam = new util.HashMap[String, AnyRef]
    searchParam.put("sublanguageid", "eng")
    searchParam.put("imdbid", cleanId)

    val response = withTokenCheck(token) { token =>
      client.execute(config, "SearchSubtitles", Array[AnyRef](token, Array(searchParam)))
    }
    val responseMap = asAnyRefMap(response)
    val data = responseMap.get("data")
    if (!data.isInstanceOf[Array[Object]]) {
      logger.debug("No data returned for id " + imdbId)
      return null
    }
    val dataArray = responseMap.get("data").asInstanceOf[Array[Object]]
    val filteredDataArray = dataArray.filter(
      subtitle => asJavaStringMap(subtitle).get("SubFormat") == "srt"
    )
    val sorted = filteredDataArray.sortWith {
      (o1, o2) => getRating(o1) > getRating(o2)
    }
    sorted
  }

  override def get(imdbId: String): List[Season] = {
    logger.info(s"Getting series details for: $imdbId")
    val candidates = getSubtitleCandidates(imdbId)
    val converted = subtitleResultsToMap(candidates, imdbId)

    val groupedBySeason = converted.groupBy { c =>
      c.getOrElse("SeriesSeason", "0").toInt
    }.filterKeys(_ != 0)

    val episodeList = mutable.Set[Episode]()
    val seasons: List[Season] =
      groupedBySeason.map { g =>
        val seasonNumber = g._1
        val byEpisodeNumber = g._2.groupBy(_.get("SeriesEpisode").get.toInt)
        byEpisodeNumber.keysIterator.foreach { k =>
          val firstEpisodeMap = byEpisodeNumber.get(k).get(0)
          val firstEpisode = getSeriesEpisode(firstEpisodeMap)
          episodeList += Episode(firstEpisode._2, seasonNumber, firstEpisode._1, imdbId)
        }
        //TODO improve this.
        val episode: (Int, String) = getSeriesEpisode(g._2.maxBy(m => getSeriesEpisode(m)._1))
        Season(imdbId, seasonNumber, episode._1)
      }.toList
       .sortBy(s => s.seasonNumber)

    episodeList.foreach(persistenceManager.saveEpisode)
    seasons
  }

  override def searchSubtitlesOnline(imdbId: String): Option[SubtitleWithContent] = {
    val downloadWindow = DateTime.now.minusHours(HOURS_PER_DOWNLOAD_PERIOD)
    if(persistenceManager.subtitleDownloadsSince(downloadWindow) >= MAX_DOWNLOADS_PER_PERIOD) {
      val next = persistenceManager.nextAvailableDownload(MAX_DOWNLOADS_PER_PERIOD, HOURS_PER_DOWNLOAD_PERIOD)
      logger.info(s"Too many subtitle downloads. Next download opportunity: $next")
      None
    } else {
      performSearchOnline(imdbId)
    }
  }

  override def downloadSubtitle(id: String): Option[String] = {
    logger.info(s"Downloading actual subtitle file for imdbId: $id")
    val response = client.execute(config, "DownloadSubtitles", Array[AnyRef](token, Array(id)))
    val responseMap = asAnyRefMap(response)
    responseMap.get("data") match {
      case bool: lang.Boolean if !bool =>
        logger.error(s"Something went wrong when downloading the subtitle with subId: $id.\n" +
          s"We might have exceeded the download limit: ${responseMap.get("status")}")
        None
      case array: Array[Object] =>
        val dataString = asAnyRefMap(array(0))
          .get("data").asInstanceOf[String]
        Some(OpenSubtitlesDecoder.decode(dataString))
    }
  }

  private def performSearchOnline(imdbId: String): Option[SubtitleWithContent] = {
    val dataArray = getSubtitleCandidates(imdbId)
    val subtitleMap = asJavaStringMap(dataArray(0))
    if (!dataArray.isEmpty) {
      val subtitleStringOption = downloadSubtitle(subtitleMap.get("IDSubtitleFile"))
      subtitleStringOption.foreach(s => persistenceManager.saveSubtitleDownload())
      val subtitleId = subtitleMap.get("IDSubtitle")
      subtitleStringOption.map(SubtitleWithContent(Subtitle(subtitleId, imdbId), _))
    } else {
      None
    }
  }

  private def getSeriesEpisode(myMap: util.Map[String, String]): (Int, String) = {
    val episode = myMap.get("SeriesEpisode")
    val imdbId = "tt" + myMap.get("IDMovieImdb")
    if(episode.isEmpty) {
      (0, imdbId)
    } else {
      (episode.toInt, imdbId)
    }
  }

  private def subtitleResultsToMap(candidates: Array[Object],
                                   imdbId: String): Array[immutable.Map[String, String]] = {
    try {
      candidates.map(asJavaStringMap(_).toMap)
    } catch {
      case NonFatal(e) =>
        logger.error(s"Unable to process open subtitle results for imdbId: $imdbId")
        Array.empty[immutable.Map[String, String]]
    }
  }

  private def withTokenCheck(myToken: String)
                            (action: (String) => Object): Object = {
    logger.debug("Checking token " + myToken)
    if(checkToken(myToken)) {
      action(myToken)
    } else {
      logger.debug("Invalid. Getting new token.")
      val newToken = login()
      logger.debug("new token: " + newToken)
      token = newToken
      action(newToken)
    }
  }

  private def getRating(o: Object): Float = {
    asJavaStringMap(o).get("SubRating").toFloat
  }

  private def cleanImdbId(imdbId: String): String = {
    if(imdbId.startsWith("tt")) imdbId.substring(2) else imdbId
  }

  private def asJavaStringMap(o: Object): util.Map[String, String] =
    o.asInstanceOf[util.Map[String, String]]

  private def asAnyRefMap(o: Object): util.Map[String, AnyRef] =
    o.asInstanceOf[util.Map[String, AnyRef]]

}