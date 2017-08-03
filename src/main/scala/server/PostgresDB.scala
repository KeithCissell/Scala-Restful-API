package database

import scalaz._
import scalaz.effect.IO
import scalaz.concurrent.Task
import Scalaz._
import doobie.imports._
import doobie.util.transactor
import scala.collection.mutable.{ArrayBuffer => AB}



object Load {
  import searchengine.SearchEngine._
  import lookitup._

  def connectToDB(database: String): Transactor[Task] = {
    DriverManagerTransactor[Task] (
      "org.postgresql.Driver",
      s"jdbc:postgresql:$database",
      database,
      ""
    )
  }

  def loadDB(database: String): LookItUp = {
    val LIU = new LookItUp
    val DB = connectToDB(database)

    // Get all users from DB and add to LIU
    val users: List[(String,String)] =
      sql"select username, password from users"
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
      sql"select search_string from searches where username = $username"
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
      sql"select title, description from results where search_string = $searchString"
      .query[(String,String)]
      .list
      .transact(DB)
      .run

    val resultsArray: AB[Result] = (for (r <- results) yield {
      val title = r._1
      val description = r._2
      new Result(title, description)
    }).to[AB]

    println(resultsArray)

    return new Search(searchString)
  }

}
