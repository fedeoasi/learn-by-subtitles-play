package controllers

import com.google.inject.{Inject, Singleton}
import org.json4s.DefaultFormats
import org.json4s.jackson.Serialization._
import persistence.{DownloadError, PersistenceManager}
import play.api.mvc.{Action, Controller}

@Singleton
class SubtitleDownloadController @Inject() (persistenceManager: PersistenceManager)
  extends Controller {

  implicit val formats = DefaultFormats ++ org.json4s.ext.JodaTimeSerializers.all

  def lastKErrors() = Action {
    val errors = persistenceManager.lastKDownloadErrors(25)
    Ok(writePretty(DownloadErrorsResponse(errors))).as(JSON)
  }
}

case class DownloadErrorsResponse(downloadErrors: Seq[DownloadError])
