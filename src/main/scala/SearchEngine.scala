// src/main/scala/milestoneproject/SearchEngine.scala
package searchengine

import scala.collection.mutable.{ArrayBuffer => AB}

object SearchEngine {

  // A general list of methods to be used by a repository
  trait Repository[A, I] {
    def isEmpty: Boolean
    def getAll: AB[A]
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
    def getAll: AB[Search] = history
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
    def mostFrequentSearch: String = {
      if (!searchHistory.isEmpty) {
        val frequencies = for (s <- searchHistory.getAll) yield {
          s -> searchHistory.getAll.count(_ == s)
        }
        frequencies.maxBy(_._2)._1.value
      } else "No Search History"
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
    def getAll: AB[User] = users.values.to[AB]
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

    def engineSearchHistory: AB[Search] = {
      (for (usr <- getAll) yield usr.searchHistory.getAll).flatten
    }

    def mostFrequentSearch: String = {
      val history = engineSearchHistory
      if (!history.isEmpty) {
        val frequencies = for (s <- history) yield {
          s -> history.count(_ == s)
        }
        frequencies.maxBy(_._2)._1.value
      } else "No Searches Found"
    }

    def changePassword(username: String, newPassword: String): Unit = get(username) match {
      case Some(user) => {
        user.password = newPassword
        update(user)
      }
      case None => println(s"Coundn't find: $username")
    }

    def validUser(u: String, p: String): Boolean = get(u) match {
      case Some(user) => user.password match {
        case up if up == p  => true
        case _              => false
      }
      case None             => false
    }
  }

}
