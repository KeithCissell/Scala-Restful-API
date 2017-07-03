package lookitup.server

import io.circe._
import org.http4s._
import org.http4s.circe._
import org.http4s.server._
import org.http4s.dsl._

object LIUService {
  val service = HttpService {
    case request @ GET -> Root / "ping" =>
      Ok(Json.obj("message" -> Json.fromString("Pong")))
    case request @ POST -> Root / "create_user" =>
      Ok(Json.obj("message" -> Json.fromString("Userrrr")))
  }
}
