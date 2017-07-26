package searchengine

import akka.actor._
import org.http4s.dsl._

import scala.collection.mutable

import lookitup.LookItUp
import searchengine.SearchEngine._


object LIUActor {

  implicit val system = ActorSystem()

  def props(searchEngine: LookItUp): Props = Props(new LIUActor(searchEngine))

  final case class CreateUser(username: String, password: String)
  final case class ChangePassword(username: String, password: String)
  final case class AddSearchHistory(username: String, searchResult: Search)
}


class LIUActor(LIU: LookItUp) extends Actor {
  import LIUActor._

  override def receive: Receive = {

    case CreateUser(username, password) =>
      LIU.create(new User(username, password))

    case ChangePassword(username, password) =>
      LIU.changePassword(username, password)

    case AddSearchHistory(username, searchResult) =>
      LIU.addSearchHistory(username, searchResult)

  }
}

object Caller {
  def props(): Props = Props(new Caller)
}

class Caller extends Actor {
  import Caller._

  override def receive: Receive = {
    case _ => 
  }
}
