package searchengine

import akka.actor._
import org.http4s.dsl._

import scala.collection.mutable
import scala.collection.mutable.{ArrayBuffer => AB}

import lookitup.LookItUp
import searchengine.SearchEngine._


object LIUActor {

  implicit val system = ActorSystem()

  def props(searchEngine: LookItUp): Props = Props(new LIUActor(searchEngine))

  final case class CreateUser(username: String, password: String)
  final case class ChangePassword(username: String, password: String)
  final case class AddSearchHistory(username: String, searchResult: Search)

  trait ActorResponse
  final case class ActorSuccess(message: String) extends ActorResponse
  final case class ActorFailure(error: Throwable) extends ActorResponse
}


class LIUActor(LIU: LookItUp) extends Actor {
  import LIUActor._

  override def receive: Receive = {

    case CreateUser(username, password) =>
      try     { LIU.create(new User(username, password)) }
      catch   { case error: Throwable => sender() ! ActorFailure(error) }
      finally { sender() ! ActorSuccess(s"[$username] user created.") }

    case ChangePassword(username, password) =>
      try     { LIU.changePassword(username, password) }
      catch   { case error: Throwable => sender() ! ActorFailure(error) }
      finally { sender() ! ActorSuccess(s"[$username] password changed.") }

    case AddSearchHistory(username, searchResult) =>
      try     { LIU.addSearchHistory(username, searchResult) }
      catch   { case error: Throwable => sender() ! ActorFailure(error) }
      finally { sender() ! ActorSuccess(s"[$username] search added to history.") }
    }

}
