package search

import model.{ Title, TitleType }
import org.json4s._

trait MovieSearcher {
  def searchTitleJson(title: String): String
  def searchTitle(title: String): Option[Title]
}

class TitleTypeSerializer
    extends CustomSerializer[TitleType](format =>
      (
        { case JString(s) => TitleType.typesByDiscriminator(s) },
        { case t: TitleType => JString(t.discriminator) }
      )
    )
