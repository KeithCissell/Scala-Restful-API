package searchengine.actors

import akka.actor._

import scala.collection.mutable

import searchengine.SearchEngine._



object SearchActor {

  implicit val system = ActorSystem()

  def props(userId: String, searchId: String): Props = Props(new SearchActor(userId, searchId))

  final case class RequestTrackSearch(userId: String, searchId: String)
  case object DeviceRegistered

  case object RequestResults
  final case class RespondResults(results: List[Result])
  final case class RecordResults(results: List[Result])
  //final case class RequestResults

}


class SearchActor(userId: String, searchId: String) extends Actor with ActorLogging {
  import SearchActor._

  var results: mutable.Map[String,Result] = mutable.Map.empty
  var searchFrequency: Int = 0

  override def preStart(): Unit = log.info("Result actor {}-{} started", userId, searchId)
  override def postStop(): Unit = log.info("Result actor {}-{} stopped", userId, searchId)

  override def receive: Receive = {
    case RequestTrackSearch(`userId`, `searchId`) =>
      sender() ! DeviceRegistered

    case RequestTrackSearch(userId, searchId) =>
      log.warning(
        "Ignoring TrackResult request for {}-{}. This actor is responsible for {}-{}.",
        userId, searchId, this.userId, this.searchId
      )

    case RequestResults =>
      sender() ! RespondResults(results.values.toList)

    case RecordResults(results) =>
      for (result <- results) {
        log.info("Recorded Result {} for {}", result.title, userId)
        this.results += (result.title -> result)
      }
  }
}
