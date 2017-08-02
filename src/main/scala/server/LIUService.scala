package lookitup.server

import io.circe._
import org.http4s._
import org.http4s.dsl._
import org.http4s.circe._
import org.http4s.server._

import org.json4s._
import org.json4s.JsonDSL._
import org.json4s.jackson.JsonMethods._

import scalaz._
import scalaz.concurrent.Task

import scala.concurrent.Await
import scala.concurrent.Future
import scala.concurrent.duration._
import scala.collection.mutable.{ArrayBuffer => AB}

import lookitup.LookItUp
import searchengine._
import searchengine.SearchEngine._
import searchengine.SearchEngine.Search
import httpclient.DuckDuckGoAPI._
import jsonhandler.Handlers._


object LIUService {

  // LookItUp search engine
  val LIU = new LookItUp


  // This service plugs into the server to handle incoming requests
  val service = HttpService {
    case req @ GET  -> Root / "ping"                    => Ok("Pong")
    case req @ POST -> Root / "create_user"             => createUser(req)
    case req @ POST -> Root / "change_password"         => changePassword(req)
    case req @ POST -> Root / "search" :? searchString  => search(req, searchString("q")(0))
    case req @ GET  -> Root / "search_terms"            => getEngineSearches
    case req @ POST -> Root / "search_terms"            => getUserSearches(req)
    case req @ GET  -> Root / "most_common_search"      => engineMostFrequentSearch
    case req @ POST -> Root / "most_common_search"      => userMostFrequentSearch(req)
  }

  def createUser(req: Request): Task[Response] = req.decode[Json]{ data =>
    val username = extractField("username", data)
    val password = extractField("password", data)
    (username != None && !LIU.contains(username) && password != None) match {
      case false  => Forbidden(data)
      case true   => {
        val task = LIU.createUser(username.get, password.get)
        task.attemptRun match {
          case \/-(message) => Ok(message)
          case -\/(error)   => ExpectationFailed(s"${error.toString}")
        }
      }
    }
  }

  def changePassword(req: Request): Task[Response] = req.decode[Json]{ data =>
    val username = extractField("username", data)
    val oldPassword = extractField("oldPassword", data)
    val newPassword = extractField("newPassword", data)
    val validation = LIU.validUser(username, oldPassword)
    validation.run match {
      case false  => Forbidden(data)
      case true   => newPassword match {
        case None   => Forbidden(data)
        case Some(n) if n != oldPassword => {
          val task = LIU.changePassword(username.get, newPassword.get)
          task.attemptRun match {
            case \/-(message) => Ok(message)
            case -\/(error)   => ExpectationFailed(s"${error.toString}")
          }
        }
      }
    }
  }

  def search(req: Request, searchString: String): Task[Response] = req.decode[Json]{ data =>
    val username = extractField("username", data)
    val password = extractField("password", data)
    val validation = LIU.validUser(username, password)
    validation.run match {
      case false  => Forbidden(data)
      case true   => {
        val task = LIU.addSearchHistory(username.get, searchString)
        task.attemptRun match {
          case \/-(searchResult)  => Ok(encodeSearch(searchResult))
          case -\/(error)         => ExpectationFailed(s"${error.toString}")
        }
      }
    }
  }

  def getEngineSearches: Task[Response] = {
    val task = LIU.engineSearchHistory
    task.attemptRun match {
      case \/-(searches)  => Ok(encodeSearches(searches))
      case -\/(error)     => ExpectationFailed(s"${error.toString}")
    }
  }

  def getUserSearches(req: Request): Task[Response] = req.decode[Json]{ data =>
    val username = extractField("username", data)
    val password = extractField("password", data)
    val validation = LIU.validUser(username, password)
    validation.run match {
      case false  => Forbidden(data)
      case true   => {
        val task = LIU.userSearchHistory(username.get)
        task.attemptRun match {
          case \/-(searches)  => Ok(encodeSearches(searches))
          case -\/(error)     => ExpectationFailed(s"${error.toString}")
        }
      }
    }
  }

  def engineMostFrequentSearch: Task[Response] = {
    val task = LIU.mostFrequentSearch
    task.attemptRun match {
      case \/-(terms) => Ok(encodeTerms(terms))
      case -\/(error) => ExpectationFailed(s"${error.toString}")
    }
  }

  def userMostFrequentSearch(req: Request): Task[Response] = req.decode[Json]{ data =>
    val username = extractField("username", data)
    val password = extractField("password", data)
    val validation = LIU.validUser(username, password)
    validation.run match {
      case false  => Forbidden(data)
      case true   => {
        val task = LIU.userMostFrequentSearch(username.get)
        task.attemptRun match {
          case \/-(terms) => Ok(encodeTerms(terms))
          case -\/(error) => ExpectationFailed(s"${error.toString}")
        }
      }
    }
  }

}
