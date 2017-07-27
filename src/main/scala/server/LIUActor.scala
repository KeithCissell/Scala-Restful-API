package searchengine

import akka.actor._
import org.http4s.dsl._

import scala.collection.mutable
import scala.collection.mutable.{ArrayBuffer => AB}

import lookitup.LookItUp
import searchengine.SearchEngine._


object LIUActor {

  implicit val system = ActorSystem()

  def props(searchEngine: LookItUp, completedRequests: AB[Int]): Props = Props(new LIUActor(searchEngine, completedRequests))

  final case class CreateUser(reqId: Int, username: String, password: String)
  final case class ChangePassword(reqId: Int, username: String, password: String)
  final case class AddSearchHistory(reqId: Int, username: String, searchResult: Search)

  final case class Completed(reqId: Int)
}


class LIUActor(LIU: LookItUp, completedRequests: AB[Int]) extends Actor {
  import LIUActor._

  override def receive: Receive = {

    case CreateUser(reqId, username, password) =>
      LIU.create(new User(username, password))
      completedRequests += reqId
      sender() ! Completed(reqId)

    case ChangePassword(reqId, username, password) =>
      LIU.changePassword(username, password)
      completedRequests += reqId
      sender() ! Completed(reqId)

    case AddSearchHistory(reqId, username, searchResult) =>
      LIU.addSearchHistory(username, searchResult)
      completedRequests += reqId
      sender() ! Completed(reqId)
  }
  
}


// Empty Actor that is used to call LIUActor
object Caller {
  def props(): Props = Props(new Caller)
}

class Caller extends Actor {
  import Caller._

  override def receive: Receive = {
    case _ =>
  }
}
