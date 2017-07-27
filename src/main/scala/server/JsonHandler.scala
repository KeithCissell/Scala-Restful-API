package jsonhandler

import io.circe._
import org.json4s._
import org.json4s.JsonDSL._
import org.json4s.jackson.JsonMethods._

import lookitup.LookItUp
import searchengine.SearchEngine._
import searchengine.SearchEngine.Search

object Handlers {

  // Retreives a field from io.circe.Json data
  def extractField(field: String, data: Json): Option[String] = {
    val cursor: HCursor = data.hcursor
    cursor.downField(field).as[String] match {
      case Left(_)  => None
      case Right(s) => Some(s)
    }
  }

  // `encode()` methods take in a class and convert them to Json
  def encodeSearch(search: Search): String = {
    val json =
      ("results" -> search.results.map { r =>
        ( ("name"         -> r.title) ~
          ("description"  -> r.description) )
      })
    return compact(render(json))
  }
  def encodeSearches(searches: Seq[Search]): String = {
    val json =
      ("searches" -> searches.map { s =>
        "term" -> s.value
      })
    return compact(render(json))
  }
  def encodeTerms(searchTerms: Seq[String]): String = {
    val json =
      ("Most Frequent Searches" -> searchTerms.map { t =>
        "term" -> t
      })
    return compact(render(json))
  }

}
