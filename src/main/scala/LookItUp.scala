// src/main/scala/milestoneproject/LookItUp.scala
package lookitup

import httpclient.DuckDuckGoAPI._
import searchengine.SearchEngine._

class LookItUp(users: Map[String,User] = Map.empty)
    extends SearchEngine("Look It Up", users) with DuckDuckGoAPI {
  // Allows LookItUp to be constructed with a Seq of users
  def this(users: Seq[User]) {
    this((users.map(_.name) zip users).toMap)
  }

  // Handle User Search Request
  def userSearch(userName: String, query: String): Unit =
    users.get(userName) match {
      case None       => println(s"User not found: '${userName}'\n")
      case Some(user) => {
        val searchResult = searchDDG(query)
        user.searchHistory.create(searchResult)
        update(user)
      }
  }
}
