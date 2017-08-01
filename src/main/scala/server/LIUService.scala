package lookitup.server

import akka.actor._
import akka.pattern.ask
import akka.util.Timeout

import io.circe._
import org.http4s._
import org.http4s.dsl._
import org.http4s.circe._
import org.http4s.server._

import org.json4s._
import org.json4s.JsonDSL._
import org.json4s.jackson.JsonMethods._

import scala.concurrent.Await
import scala.concurrent.Future
import scalaz.concurrent.Task
import scala.concurrent.duration._
import scala.collection.mutable.{ArrayBuffer => AB}

import lookitup.LookItUp
import searchengine._
import searchengine.LIUActor._
import searchengine.SearchEngine._
import searchengine.SearchEngine.Search
import httpclient.DuckDuckGoAPI._
import jsonhandler.Handlers._


object LIUService {

  implicit val system = ActorSystem("LookItUp")
  implicit val timeout = Timeout(5 seconds)
  val LIU = new LookItUp
  val liuActor = system.actorOf(LIUActor.props(LIU))


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
        val future: Future[ActorResponse] = ask(liuActor, CreateUser(username.get, password.get)).mapTo[ActorResponse]
        val result = Await.result(future, 5 seconds)
        result match {
          case ActorSuccess(message)  => Ok(message)
          case ActorFailure(error)    => ExpectationFailed(s"Could not create user (username.get):\n$error")
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
          val future: Future[ActorResponse] = ask(liuActor, ChangePassword(username.get, newPassword.get)).mapTo[ActorResponse]
          val result = Await.result(future, 5 seconds)
          result match {
            case ActorSuccess(message)  => Ok(message)
            case ActorFailure(error)    => ExpectationFailed(s"Could not change password for (username.get):\n$error")
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
        val future: Future[ActorResponse] = ask(liuActor, AddSearchHistory(username.get, searchResult)).mapTo[ActorResponse]
        val result = Await.result(future, 5 seconds)
        result match {
          case ActorSuccess(message)  => Ok(message)
          case ActorFailure(error)    => ExpectationFailed(s"Could not create user (username.get):\n$error")
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

}
