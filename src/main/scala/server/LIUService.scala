package lookitup.server

import io.circe._
import org.http4s._
import org.http4s.dsl._
import org.http4s.circe._
import org.http4s.server._

import scalaz._
import scalaz.concurrent.Task

import scala.concurrent.duration._
import scala.collection.mutable.{ArrayBuffer => AB}

import lookitup.LookItUp

import searchengine.SearchEngine._

import httpclient.DuckDuckGoAPI._

import jsonhandler.Handlers._

import doobie.imports._

import database.Connect._
import database.Load._
import database.Edit._


object LIUService {

  // Database and local LookItUp data structure
  var DB: Transactor[Task] = connectToDB("lookitup")
  var LIU: LookItUp = new LookItUp


  // This service plugs into the server to handle incoming requests
  val service = HttpService {
    case req @ GET  -> Root / "ping"                    => Ok("Pong")
    case req @ POST -> Root / "connect_to_database"     => connectToDatabase(req)
    case req @ POST -> Root / "create_user"             => createUser(req)
    case req @ POST -> Root / "change_password"         => changePassword(req)
    case req @ POST -> Root / "search" :? searchString  => search(req, searchString("q")(0))
    case req @ GET  -> Root / "search_terms"            => getEngineSearches
    case req @ POST -> Root / "search_terms"            => getUserSearches(req)
    case req @ GET  -> Root / "most_common_search"      => engineMostFrequentSearch
    case req @ POST -> Root / "most_common_search"      => userMostFrequentSearch(req)
    case req @ GET  -> Root / "clear_database"          => clearDB
  }

  def connectToDatabase(req: Request): Task[Response] = req.decode[Json]{ data =>
    val database = extractField("database", data)
    database match {
      case None     => ExpectationFailed()
      case Some(db) => {
        DB = connectToDB(db)
        LIU = loadDB(DB)
        Ok(s"Connected to Database: $database")
      }
    }
  }

  def createUser(req: Request): Task[Response] = req.decode[Json]{ data =>
    val username = extractField("username", data)
    val password = extractField("password", data)
    (username != None && !LIU.contains(username) && password != None) match {
      case false  => Forbidden(data)
      case true   => {
        val task = LIU.createUser(username.get, password.get)
        task.attemptRun match {
          case -\/(error)   => ExpectationFailed(s"${error.toString}")
          case \/-(user) => {
            val dbEdit = addUserDB(user, DB).run
            Ok(s"[$username] user created.")
          }
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
            case -\/(error)   => ExpectationFailed(s"${error.toString}")
            case \/-(message) => {
              val dbEdit = changePasswordDB(username.get, newPassword.get, DB).run
              Ok(message)
            }
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
          case -\/(error)         => ExpectationFailed(s"${error.toString}")
          case \/-(searchResult)  => {
            val dbEdit = addSearchDB(username.get, searchResult, DB).run
            Ok(encodeSearch(searchResult))
          }
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

  def clearDB: Task[Response] = {
    val task = clearAllTables(DB)
    task.attemptRun match {
      case -\/(error) => ExpectationFailed(s"${error.toString}")
      case \/-(_) => {
        LIU = loadDB(DB)
        Ok("Datatbase Cleared")
      }
    }
  }

}
