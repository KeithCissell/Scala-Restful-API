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

  final case class CreateUser(reqId: Int, username: String, password: String)
  final case class ChangePassword(reqId: Int, username: String, password: String)
  final case class AddSearchHistory(reqId: Int, username: String, searchResult: Search)
}


class LIUActor(LIU: LookItUp) extends Actor {
  import LIUActor._

  var completedRequests: AB[Int] = AB.empty

  override def receive: Receive = {

    case CreateUser(reqId, username, password) =>
      LIU.create(new User(username, password))
      completedRequests += reqId

    case ChangePassword(reqId, username, password) =>
      LIU.changePassword(username, password)
      completedRequests += reqId

    case AddSearchHistory(reqId, username, searchResult) =>
      LIU.addSearchHistory(username, searchResult)
      completedRequests += reqId

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
