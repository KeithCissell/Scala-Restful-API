package lookitup.server

import io.circe._
import org.http4s._
import org.http4s.dsl._
import org.http4s.circe._
import org.http4s.server._
import scalaz.concurrent.Task

import lookitup.LookItUp

object LIUService {

  val LIU = new LookItUp
  var userCount = 0

  def createUser(req: Request): Task[Response] = {
    println(req)
    userCount = userCount + 1
    Ok(Json.obj("message" -> Json.fromString(s"Users: $userCount")))
  }

  val service = HttpService {
    case req @ GET -> Root / "ping" =>
      Ok()

    case req @ POST -> Root / "create_user" =>
      createUser(req)

    case req @ POST -> Root / "change_password" =>
      Ok(Json.obj("message" -> Json.fromString("Change Password")))

    case req @ POST -> Root / "search?q=:searchString" =>
      Ok(Json.obj("message" -> Json.fromString("Search")))

    case req @ GET -> Root / "search_terms" =>
      Ok(Json.obj("message" -> Json.fromString("All Searches")))

    case req @ POST -> Root / "search_terms" =>
      Ok(Json.obj("message" -> Json.fromString("User Searches")))

    case req @ GET -> Root / "most_common_search" =>
      Ok(Json.obj("message" -> Json.fromString("All Common Searches")))

    case req @ POST -> Root / "most_common_search" =>
      Ok(Json.obj("message" -> Json.fromString("User Common Searches")))
  }

}
