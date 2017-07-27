package lookitup.server

import akka.actor._
import io.circe._
import org.http4s._
import org.http4s.dsl._
import org.http4s.circe._
import org.http4s.server._
import org.json4s._
import org.json4s.JsonDSL._
import org.json4s.jackson.JsonMethods._
import scalaz.concurrent.Task
import scala.collection.mutable.{ArrayBuffer => AB}


import lookitup.LookItUp
import httpclient.DuckDuckGoAPI._
import searchengine._
import searchengine.SearchEngine._
import searchengine.SearchEngine.Search
import jsonhandler.Handlers._


object LIUService {

  implicit val system = ActorSystem()

  class IdGenerator(var idCounter: Int = 0) {
    def getNext: Int = {
      idCounter += 1
      return idCounter
    }
  }

  val idGenerator = new IdGenerator
  val LIU = new LookItUp
  var completedRequests: AB[Int] = AB.empty

  val caller = system.actorOf(Caller.props())
  val liuActor = system.actorOf(LIUActor.props(LIU, completedRequests))


  // This service plugs into the server to handle incoming requests
  val service = HttpService {
    case req @ GET  -> Root / "ping"                    => Ok("Pong")
    case req @ POST -> Root / "create_user"             => createUser(req)
    case req @ POST -> Root / "change_password"         => changePassword(req)
    case req @ POST -> Root / "search" :? searchString  => search(req, searchString("q")(0))
    case req @ GET  -> Root / "search_terms"            => Ok(encodeSearches(LIU.engineSearchHistory))
    case req @ POST -> Root / "search_terms"            => getUserSearches(req)
    case req @ GET  -> Root / "most_common_search"      => Ok(encodeTerms(LIU.mostFrequentSearch))
    case req @ POST -> Root / "most_common_search"      => userMostFrequentSearch(req)
  }

  def createUser(req: Request): Task[Response] = req.decode[Json]{ data =>
    val username = extractField("username", data)
    val password = extractField("password", data)
    LIU.contains(username) match {
      case true   => Forbidden(data)
      case false  => {
        val reqId = idGenerator.getNext
        println(s"Request received: $reqId")
        liuActor.tell(LIUActor.CreateUser(reqId, username.get, password.get), caller)
        awaitCompletion(reqId) match {
          case true   => Ok(data)
          case false  => ExpectationFailed(data)
        }
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
        case None   => Forbidden(data)
        case Some(n) if n != oldPassword => {
          val reqId = idGenerator.getNext
          println(s"Request received: $reqId")
          liuActor.tell(LIUActor.ChangePassword(reqId, username.get, n), caller)
          awaitCompletion(reqId) match {
            case true   => Ok(data)
            case false  => ExpectationFailed(data)
          }
        }
      }
    }
  }

  def search(req: Request, searchString: String): Task[Response] = req.decode[Json]{ data =>
    val username = extractField("username", data)
    val password = extractField("password", data)
    LIU.validUser(username, password) match {
      case false  => Forbidden(data)
      case true   => {
        val searchResult = LIU.searchDDG(searchString)
        val reqId = idGenerator.getNext
        println(s"Request received: $reqId")
        liuActor.tell(LIUActor.AddSearchHistory(reqId, username.get, searchResult), caller)
        awaitCompletion(reqId) match {
          case true   => Ok(encodeSearch(searchResult))
          case false  => ExpectationFailed(data)
        }
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

  def userMostFrequentSearch(req: Request): Task[Response] = req.decode[Json]{ data =>
    val username = extractField("username", data)
    val password = extractField("password", data)
    LIU.validUser(username, password) match {
      case false  => Forbidden(data)
      case true   => Ok(encodeTerms(LIU.users(username.get).mostFrequentSearch))
    }
  }

  // Repeatedly attemps to find reqId in the completedRequests list
  // Times out and returns false after 1 minute
  def awaitCompletion(reqId: Int): Boolean = {
    for (i <- 0 until 600) {
      if (completedRequests.contains(reqId)) return true
      else Thread.sleep(100)
    }
    return false
  }

}
