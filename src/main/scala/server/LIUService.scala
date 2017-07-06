package lookitup.server

import io.circe._
import org.http4s._
import org.http4s.dsl._
import org.http4s.circe._
import org.http4s.server._
import scalaz.concurrent.Task

import org.json4s._
import org.json4s.JsonDSL._
import org.json4s.jackson.JsonMethods._

import lookitup.LookItUp
import httpclient.DuckDuckGoAPI._
import searchengine.SearchEngine._
import searchengine.SearchEngine.Search


object LIUService {

  val LIU = new LookItUp

  // This service plugs into the server to handle incoming requests
  val service = HttpService {
    case req @ GET  -> Root / "ping"                    => Ok()
    case req @ POST -> Root / "create_user"             => createUser(req)
    case req @ POST -> Root / "change_password"         => changePassword(req)
    case req @ POST -> Root / "search" :? searchString  => search(req, searchString("q")(0))
    case req @ GET  -> Root / "search_terms"            => Ok(encodeSearches(LIU.engineSearchHistory))
    case req @ POST -> Root / "search_terms"            => getUserSearches(req)
    case req @ GET  -> Root / "most_common_search"      => Ok(encodeTerms(LIU.mostFrequentSearch))
    case req @ POST -> Root / "most_common_search"      => Ok(Json.obj("message" -> Json.fromString("User Common Searches")))
  }

  // Retreives a field from io.circe.Json data
  def extractField(field: String, data: Json): Option[String] = {
    val cursor: HCursor = data.hcursor
    cursor.downField(field).as[String] match {
      case Left(_)  => None
      case Right(s) => Some(s)
    }
  }

  // `encode()` methods take in a class and convert them to Json
  def encode(search: Search): String = {
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

  def createUser(req: Request): Task[Response] = req.decode[Json]{ data =>
    val username = extractField("username", data)
    val password = extractField("password", data)
    LIU.contains(username) match {
      case true   =>Forbidden(data)
      case false  => {
        LIU.create(new User(username.get, password.get))
        Ok(data)
      }
    }
  }

  def changePassword(req: Request): Task[Response] = req.decode[Json]{ data =>
    val username = extractField("username", data)
    val oldPassword = extractField("oldPassword", data)
    val newPassword = extractField("newPassword", data)
    LIU.validUser(username, oldPassword) match {
      case false  => Forbidden(data)
      case true   => newPassword match {
        case Some(n) if n != oldPassword => {
          LIU.changePassword(username.get, n)
          Ok(data)
        }
        case _ => Forbidden(data)
      }
    }
  }

  def search(req: Request, searchString: String): Task[Response] = req.decode[Json]{ data =>
    val username = extractField("username", data)
    val password = extractField("password", data)
    LIU.validUser(username, password) match {
      case false  => Forbidden(data)
      case true   => LIU.userSearch(username.get, searchString) match {
        case Some(searchResult) => Ok(encode(searchResult))
        case None               => Forbidden(data)
      }
    }
  }

  def getUserSearches(req: Request): Task[Response] = req.decode[Json]{ data =>
    val username = extractField("username", data)
    val password = extractField("password", data)
    LIU.validUser(username, password) match {
      case false  => Forbidden(data)
      case true   => Ok(encodeSearches(LIU.users(username.get).searchHistory.getAll))
    }
  }

  // def userMostFrequentSearch(req: Request): Unit

}
