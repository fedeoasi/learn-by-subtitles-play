package config

object Config {
  val elasticSearchClusterName = "lbs"
  val indexName = "my-index"
  val movieIndexName = "movie"
  val connectionTimeout = 30000 //in milliseconds
  val episodeDownloadingEnabled = true
  val millisBetweenDownloads = 60000
  val searchResultOffset = 5
}
