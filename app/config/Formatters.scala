package config

object Formatters {
  def formatInt(i: Int): String = i.toString.replaceAll(",", "")
}
