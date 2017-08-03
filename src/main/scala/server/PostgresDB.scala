package database

import scalaz._
import scalaz.concurrent.Task
import Scalaz._
import doobie.imports._

import lookitup._
import searchengine.SearchEngine._
import scala.collection.mutable.{ArrayBuffer => AB}


object Connect {

  def connectToDB(database: String): Transactor[Task] = {
    DriverManagerTransactor[Task] (
      "org.postgresql.Driver",
      s"jdbc:postgresql:$database",
      database,
      ""
    )
  }

}


object Load {

  def loadDB(DB: Transactor[Task]): LookItUp = {
    val LIU = new LookItUp

    // Get all users from DB and add to LIU
    val users: List[(String,String)] =
      sql"SELECT username, password FROM users"
      .query[(String,String)]
      .list
      .transact(DB)
      .run

    for (u <- users) {
      val username = u._1
      val password = u._2
      val user = loadUser(username, password, DB)
      LIU.create(user)
    }

    return LIU
  }

  def loadUser(username: String, password: String, DB: Transactor[Task]): User = {
    val searches: List[String] =
      sql"SELECT search_string FROM searches WHERE username = $username"
      .query[String]
      .list
      .transact(DB)
      .run

    val searchArray: AB[Search] = (for (s <- searches) yield loadSearch(s, DB)).to[AB]
    val searchHistory = new SearchHistory(searchArray)

    return new User(username, password, searchHistory)
  }

  def loadSearch(searchString: String, DB: Transactor[Task]): Search = {
    val results: List[(String,String)] =
      sql"SELECT title, description FROM results WHERE search_string = $searchString"
      .query[(String,String)]
      .list
      .transact(DB)
      .run

    val resultsArray: AB[Result] = (for (r <- results) yield {
      val title = r._1
      val description = r._2
      new Result(title, description)
    }).to[AB]

    return new Search(searchString, resultsArray)
  }

}


object Edit {

  def addUserDB(user: User, DB: Transactor[Task]): Task[Unit] = Task {
    val username = user.name
    val password = user.password
    sql"INSERT INTO users VALUES ($username, $password)"
      .update.run
      .transact(DB)
      .run
  }

  def changePasswordDB(username: String, newPassword: String, DB: Transactor[Task]): Task[Unit] = Task {
    sql"UPDATE users SET password = $newPassword WHERE username = $username"
      .update.run
      .transact(DB)
      .run
  }

  def addSearchDB(username: String, search: Search, DB: Transactor[Task]): Task[Unit] = Task {
    val searchString = search.value
    val results = search.results

    sql"INSERT INTO searches VALUES ($username, $searchString)"
      .update.run
      .transact(DB)
      .run

    for (r <- results) addResultDB(searchString, r, DB).run
  }

  def addResultDB(searchString: String, result: Result, DB: Transactor[Task]): Task[Unit] = Task {
    val title = result.title
    val description = result.description

    sql"INSERT INTO results VALUES ($searchString, $title, $description)"
      .update.run
      .transact(DB)
      .run
  }



  def clearAllTables(DB: Transactor[Task]): Task[Unit] = Task {
    sql"DELETE FROM users"
      .update.run
      .transact(DB)
      .run
    sql"DELETE FROM searches"
      .update.run
      .transact(DB)
      .run
    sql"DELETE FROM results"
      .update.run
      .transact(DB)
      .run
  }

}
