package search

import java.net.InetSocketAddress
import java.text.SimpleDateFormat
import java.util

import model.{Movie, IMovie, SubEntry}
import org.elasticsearch.action.admin.indices.create.CreateIndexRequestBuilder
import org.elasticsearch.action.admin.indices.exists.indices.IndicesExistsRequest
import org.elasticsearch.action.get.{MultiGetItemResponse, MultiGetResponse}
import org.elasticsearch.action.search.SearchType
import org.elasticsearch.client.IndicesAdminClient
import org.elasticsearch.client.transport.TransportClient
import org.elasticsearch.common.settings.Settings
import org.elasticsearch.common.transport.InetSocketTransportAddress
import org.elasticsearch.index.query.QueryBuilders
import org.elasticsearch.search.SearchHit
import org.elasticsearch.search.suggest.completion.{CompletionSuggestion, CompletionSuggestionBuilder}
import org.json4s.JsonDSL._
import org.json4s.jackson.JsonMethods._

import scala.collection.JavaConversions._

trait SearchInteractor {
  def ensureIndexExists(name: String, setup: CreateIndexRequestBuilder => Unit = s => Unit): Boolean
  def indexSubtitleContent(index: String, text: String, subtitleId: String, movieId: String, flush: Boolean): Boolean
  def indexSubtitleEntry(index: String, entry: SubEntry, subtitleId: String, imdbId: String, flush: Boolean): Boolean
  def indexSubtitleEntries(index: String, entries: List[SubEntry], s1: String, s2: String): Boolean
  def searchSubtitleEntries(index: String, query: String): List[SubEntrySearchResult]
  def searchSubtitles(index: String, query: String): List[SubtitleSearchResult]
  def getSubtitleEntries(index: String, ids: List[String], scores: List[Float]): List[SubEntrySearchResult]
  def suggestMovie(index: String, query: String): List[String]
  def suggestMovieFull(index: String, query: String): List[SuggestionResult]
  def deleteIndex(name: String)
  def flushIndex(name: String)
  def close()
}

class ElasticSearchInteractor extends SearchInteractor {
  private val settings = Settings.settingsBuilder()
    .put("cluster.name", "lbs").build()

  protected val client = TransportClient.builder
    .settings(settings)
    .build()
    .addTransportAddress(new InetSocketTransportAddress(new InetSocketAddress("localhost", 9300)))

  val format = new SimpleDateFormat("HH:mm:ss,SSS")

  def ensureIndexExists(name: String, setup: CreateIndexRequestBuilder => Unit = s => Unit): Boolean = {
    val request = new IndicesExistsRequest()
    request.indices(Array[String](name))
    val indicesClient: IndicesAdminClient = client.admin().indices()
    if(indicesClient.exists(request)
      .actionGet().isExists) {
      true
    } else {
      val indexSettings = Settings.settingsBuilder()
        .put("number_of_shards", 1)
        .put("number_of_replicas", 0)
        .build()

      val createIndexBuilder = indicesClient.prepareCreate(name)
      setup(createIndexBuilder)
      createIndexBuilder.setSettings(indexSettings)
      val createIndexResponse = createIndexBuilder.execute().actionGet()
      createIndexResponse.isAcknowledged
    }
  }

  def ensureMovieIndex(indexName: String): Boolean = {
    ensureIndexExists(indexName, b => {
      b.addMapping("movie",
        """
          |{
          | "movie": {
          |   "properties": {
          |     "id" : { "type" : "string" },
          |     "title" : { "type" : "string" },
          |     "title_suggest" : {
          |       "type" : "completion",
          |       "analyzer" : "standard",
          |       "search_analyzer" : "standard",
          |       "payloads" : true
          |     }
          |   }
          | }
          |}
        """.stripMargin)
    })
  }

  def indexSubtitleEntry(index: String, entry: SubEntry, subtitleId: String, movieId: String, flush: Boolean): Boolean = {
    val json = ("number" -> entry.number) ~
      ("start" -> format.format(entry.start)) ~
      ("stop" -> format.format(entry.stop)) ~
      ("text" -> entry.text) ~
      ("subtitleId" -> subtitleId) ~
      ("imdbId" -> movieId)
    val response = client.prepareIndex(index, "entry")
      .setSource(compact(render(json)))
      .execute()
      .actionGet()
    if(flush) {
      client.admin().indices().prepareFlush(index).execute().actionGet()
    }
    response.getId != null
  }

  def indexIMovie(index: String, imovie: IMovie, flush: Boolean): Boolean = {
    indexMovie(index, imovie.imdbId, imovie.title, flush)
  }

  def indexMovie(index: String, id: String, title: String, flush: Boolean): Boolean = {
    val json = ("id" -> id) ~ ("title" -> title) ~ ("title_suggest" -> ("input" -> title) ~ ("payload" -> id))
    val compactJson = compact(render(json))
    val response = client.prepareIndex(index, "movie")
      .setSource(compactJson)
      .execute()
      .actionGet()
    if(flush) {
      client.admin().indices().prepareFlush(index).execute().actionGet()
    }
    response.getId != null
  }

  override def suggestMovie(index: String, query: String): List[String] = {
    suggestMovieFull(index, query).map(_.title)
  }


  override def suggestMovieFull(index: String, query: String): List[SuggestionResult] = {
    val suggestBuilder = client.prepareSuggest(index)
    val suggestionBuilder = new CompletionSuggestionBuilder("movie")
      .size(10)
      .field("title_suggest")
      .text(query)
    suggestBuilder
      .addSuggestion(suggestionBuilder)

    val results = suggestBuilder.execute().actionGet()
    val suggestion = results.getSuggest.getSuggestion("movie").asInstanceOf[CompletionSuggestion]
    val optionalMovie = Option(suggestion).map { s =>
      val options = suggestion.getEntries.head.getOptions
      options.map { o =>
        SuggestionResult(o.getPayloadAsString, o.getText.string(), o.getScore)
      }.sortBy(_.title.length).toList
    }
    optionalMovie.getOrElse(List.empty[SuggestionResult])
  }

  def indexSubtitleEntries(index: String, entries: List[SubEntry], subtitleId: String, movieId: String): Boolean = {
    entries.foreach(indexSubtitleEntry(index, _, subtitleId, movieId, flush = false))
    client.admin().indices().prepareFlush(index).execute().actionGet()
    true
  }
  def indexSubtitleContent(index: String, text: String, subtitleId: String, movieId: String, flush: Boolean): Boolean = {
    val json = ("subtitleId" -> subtitleId) ~
      ("movieId" -> movieId) ~
      ("text" -> text)
    val response = client.prepareIndex(index, "subtitle")
      .setSource(compact(render(json)))
      .execute()
      .actionGet()
    if(flush) {
      client.admin().indices().prepareFlush(index).execute().actionGet()
    }
    response.getId != null
  }

  def searchSubtitleEntries(index: String, query: String): List[SubEntrySearchResult] = {
    val response = client.prepareSearch(index)
      .setTypes("entry")
      .setSearchType(SearchType.DFS_QUERY_THEN_FETCH)
      .addHighlightedField("text", 0, 0)
      .setQuery(QueryBuilders.termQuery("text", query))
      .execute()
      .actionGet()
    val ids = response.getHits.map(_.getId)
    val scores = response.getHits.map(_.getScore)
    if(ids.isEmpty) {
      List[SubEntrySearchResult]()
    } else {
      getSubtitleEntries(index, ids.toList, scores.toList)
    }
  }

  def searchSubtitles(index: String, query: String): List[SubtitleSearchResult] = {
    val response = client.prepareSearch(index)
      .setTypes("subtitle")
      .setSearchType(SearchType.DFS_QUERY_THEN_FETCH)
      .addField("subtitleId")
      .addField("movieId")
      .addHighlightedField("text", 0, 0)
      .setQuery(QueryBuilders.matchPhraseQuery("text", query))
      .execute()
      .actionGet()
    val hits = response.getHits
    hits.map {
      hit =>
        SubtitleSearchResult(hit.getHighlightFields.get("text").getFragments()(0).string(),
        extractStringField(hit, "subtitleId"),
        extractStringField(hit, "movieId"),
        hit.getScore)
    }.toList
  }

  def extractStringField(hit: SearchHit, key: String): String = {
    hit.getFields.get(key).getValue.toString
  }

  def close() {
    client.close()
  }

  def deleteIndex(name: String) {
    val indicesClient: IndicesAdminClient = client.admin().indices()
    val deleteIndexBuilder = indicesClient.prepareDelete(name)
    val deleteIndexResponse = deleteIndexBuilder.execute().actionGet()
    deleteIndexResponse.isAcknowledged
  }

  def getSubtitleEntries(index: String, ids: List[String], scores: List[Float]): List[SubEntrySearchResult] = {
    val response: MultiGetResponse = client.prepareMultiGet().add(index, "entry", ids).execute().actionGet()
    val getResponseIterator: util.Iterator[MultiGetItemResponse] = response.iterator()
    //mapToSubtitleEntry(r.getResponse.getSourceAsMap())
    val entries = getResponseIterator.zip(scores.iterator).map {
      e => mapToSubtitleEntry(e._1.getResponse.getSourceAsMap, e._2)
    }
    entries.toList
  }

  def mapToSubtitleEntry(entryMap: java.util.Map[String, Object], score: Float): SubEntrySearchResult = {
    val entry: SubEntry = SubEntry(entryMap.get("number").asInstanceOf[Int],
      format.parse(entryMap.get("start").asInstanceOf[String]),
      format.parse(entryMap.get("stop").asInstanceOf[String]),
      entryMap.get("text").asInstanceOf[String])
    SubEntrySearchResult(entry, entryMap.get("subtitleId").asInstanceOf[String],
      entryMap.get("movieId").asInstanceOf[String], score)
  }

  def flushIndex(name: String) {
    client.admin().indices().prepareFlush(name).execute().actionGet()
  }

  def findEntriesForMovie(index: String, imdbId: String): List[String] = {
    val response = client.prepareSearch(index)
      .setTypes("subtitle")
      .setSearchType(SearchType.DFS_QUERY_THEN_FETCH)
      .setQuery(QueryBuilders.matchQuery("movieId", imdbId))
      .execute()
      .actionGet()
    val hits = response.getHits
    hits.map(_.getId).toList
  }
}

case class SubEntrySearchResult(entry: SubEntry, subtitleId: String, movieId: String,
                                score: Float)

case class SubtitleSearchResult(highlightedText: String, subtitleId: String, movieId: String,
                                score: Float)

case class DisplayableSubtitleResult(highlightedText: String, subtitleId: String, movie: Movie,
                                score: Float, entries: List[SubEntry])

case class SearchSubtitleResult(subtitleId: String, title: String,
                                score: Float, subEntries: List[SubEntry])

case class SuggestionResult(id: String, title: String, score: Float)