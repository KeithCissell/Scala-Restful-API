// src/main/scala/milestoneproject/SearchEngine.scala
package searchengine

import scala.collection.mutable.{ArrayBuffer => AB}


object SearchEngine {

  // A general list of methods to be used by a repository
  trait Repository[A, I] {
    def isEmpty: Boolean
    def getAll: Seq[A]
    def get(id: I): Option[A]
    def create(x: A): Unit
    def update(x: A): Unit
    def delete(x: A): Unit
  }

  // Holds the title and brief description of an search result
  case class Result(title: String, description: String)

  // Holds the query searched and a list of results returned
  case class Search(value: String, results: AB[Result] = AB())

  // A list of searches that can be viewed and manipulated
  case class SearchHistory(private var history: AB[Search] = AB())
      extends Repository[Search,Int] {
    def isEmpty: Boolean = history.isEmpty
    def contains(s: Search): Boolean = history.contains(s)
    def getAll: Seq[Search] = history
    def get(id: Int): Option[Search] = {
      if (id >= 0 && id < history.length) Some(history(id)) else None
    }
    def create(s: Search): Unit = history += s
    def update(s: Search): Unit = {
      for (i <- 0 until history.length) if (history(i).value == s.value) {
        history.update(i, s)
      }
    }
    def delete(s: Search): Unit = history -= s
  }

  // A search engine user that holds name, password and search history
  case class User(val name: String, var password: String,
      var searchHistory: SearchHistory = SearchHistory()) {
    def mostFrequentSearch: Seq[String] = {
      val searches = searchHistory.getAll
      if (!searches.isEmpty) {
        var results: AB[String] = AB.empty
        val frequencies = searches.groupBy(_.value).mapValues(_.size)
        val mostFrequent = frequencies.maxBy(_._2)
        for ((s,f) <- frequencies) if (f == mostFrequent._2) results += s
        return results.toSeq
      } else Seq.empty
    }
    override def toString: String = {
      if (searchHistory.isEmpty) s"${name}'s Search History\nEmpty"
      else s"${name}'s Search History\n$searchHistory"
    }
  }

  // A group of search engine users
  class UserGroup(var users: Map[String,User] = Map.empty) extends Repository[User,String] {
    // Allows UserGroup to be constructed with a Seq of users
    def this(users: Seq[User]) {
      this((users.map(_.name) zip users).toMap)
    }
    def isEmpty: Boolean = users.isEmpty
    def contains(id: String): Boolean = users.contains(id)
    def contains(optionID: Option[String]): Boolean = optionID match {
      case Some(id) => contains(id)
      case None     => false
    }
    def getAll: Seq[User] = users.values.toSeq
    def get(id: String): Option[User] = if (users.contains(id)) Some(users(id)) else None
    def create(u: User): Unit = {
      if (users.contains(u.name)) println(s"User already exists: ${u.name}")
      else users = users + (u.name -> u)
    }
    def update(u: User): Unit = users = users + (u.name -> u)
    def delete(u: User): Unit = if (users.contains(u.name)) users = users - u.name
  }

  // A search engine that holds a UserGroup
  class SearchEngine(val name: String, users: Map[String,User] = Map.empty)
      extends UserGroup(users) {
    // Allows SearchEngine to be constructed with a Seq of users
    def this(name: String, users: Seq[User]) {
      this(name, (users.map(_.name) zip users).toMap)
    }

    def engineSearchHistory: Seq[Search] = {
      (for (usr <- getAll) yield usr.searchHistory.getAll).flatten
    }

    def mostFrequentSearch: Seq[String] = {
      val searches = engineSearchHistory
      if (!searches.isEmpty) {
        var results: AB[String] = AB.empty
        val frequencies = searches.groupBy(_.value).mapValues(_.size)
        val mostFrequent = frequencies.maxBy(_._2)
        for ((s,f) <- frequencies) if (f == mostFrequent._2) results += s
        return results.toSeq
      } else Seq.empty
    }

    def changePassword(username: String, newPassword: String): Unit = get(username) match {
      case Some(user) => {
        user.password = newPassword
        update(user)
      }
      case None => println(s"Coundn't find: $username")
    }

    def validUser(username: Option[String], password: Option[String]): Boolean = {
      (username, password) match {
        case (Some(u),Some(p)) if contains(u) => get(u) match {
          case Some(user) => user.password == p
          case None       => false
        }
        case _ => false
      }
    }

  }

}
