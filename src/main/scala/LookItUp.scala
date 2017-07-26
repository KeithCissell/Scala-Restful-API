// src/main/scala/milestoneproject/LookItUp.scala
package lookitup

import httpclient.DuckDuckGoAPI._
import searchengine.SearchEngine._

import scala.collection.mutable


class LookItUp(users: mutable.Map[String,User] = mutable.Map.empty)
    extends SearchEngine("Look It Up", users) with DuckDuckGoAPI {
  // Allows LookItUp to be constructed with a Seq of users
  def this(users: Seq[User]) {
    this(mutable.Map(users.map(user => (user.name, user)): _*))
  }

  // Handle User Search Request
  def addSearchHistory(username: String, searchResult: Search) =
    get(username) match {
      case None       => None
      case Some(user) => {
        user.searchHistory.create(searchResult)
        update(user)
      }
  }
}
