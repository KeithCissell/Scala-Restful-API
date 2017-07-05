package lookitup.server

import io.circe._
import org.http4s._
import org.http4s.dsl._
import org.http4s.circe._
import org.http4s.server._
import scalaz.concurrent.Task

import lookitup.LookItUp
import searchengine.SearchEngine._

object LIUService {

  val LIU = new LookItUp

  // This service plugs into the server to handle incoming requests
  val service = HttpService {
    case req @ GET  -> Root / "ping"                    => Ok()
    case req @ POST -> Root / "create_user"             => createUser(req)
    case req @ POST -> Root / "change_password"         => changePassword(req)
    case req @ POST -> Root / "search" :? searchString  => Ok(Json.obj("message" -> Json.fromString("Search")))
    case req @ GET  -> Root / "search_terms"            => Ok(Json.obj("message" -> Json.fromString("All Searches")))
    case req @ POST -> Root / "search_terms"            => Ok(Json.obj("message" -> Json.fromString("User Searches")))
    case req @ GET  -> Root / "most_common_search"      => Ok(Json.obj("message" -> Json.fromString("All Common Searches")))
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

  def createUser(req: Request): Task[Response] = req.decode[Json]{ data =>
    val username = extractField("username", data)
    val password = extractField("password", data)
    (username, password) match {
      case (Some(u), Some(p)) if !LIU.contains(u) =>
        LIU.create(new User(u, p))
        Ok(data)
      case _ => Forbidden(data)
    }
  }

  def changePassword(req: Request): Task[Response] = req.decode[Json]{ data =>
    val username = extractField("username", data)
    val oldPassword = extractField("oldPassword", data)
    val newPassword = extractField("newPassword", data)
    (username, oldPassword, newPassword) match {
      case (Some(u),Some(o),Some(n)) => LIU.validUser(u, o) match {
        case false  => Forbidden(data)
        case true   =>
          LIU.changePassword(u, n)
          Ok(data)
      }
      case _ => Forbidden(data)
    }
  }

}
