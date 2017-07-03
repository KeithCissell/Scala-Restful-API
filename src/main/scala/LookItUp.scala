// src/main/scala/milestoneproject/LookItUp.scala
package milestoneproject

import httpclient.DuckDuckGo._
import searchengine.SearchEngine._
import scala.collection.mutable.{ArrayBuffer => AB}


object LookItUp {

  class LookItUp(userGroup: UserGroup = new UserGroup())
        extends SearchEngine("Look It Up", userGroup) with DuckDuckGoAPI {
    // Handle User Search Request
    def userSearch(userName: String, query: String): Unit =
      userGroup.get(userName) match {
        case None       => println(s"User not found: '${userName}'\n")
        case Some(user) => {
          val searchResult = searchDDG(query)
          user.searchHistory.create(searchResult)
          userGroup.update(user)
        }
    }
  }

}
