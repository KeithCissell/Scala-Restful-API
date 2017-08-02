package database

import scalaz._
import scalaz.effect.IO
import scalaz.concurrent.Task
import Scalaz._
import doobie.imports._

object Connect {

  def connectToDB(database: String) {
    val DB = DriverManagerTransactor[Task] (
      "org.postgresql.Driver",
      "jdbc:postgresql:postgres",
      database,
      ""
    )

    val email = sql"select recipient from emails where sender = 'Ping'"
      .query[String]
      .unique
      .transact(DB)
      .run

    println(DB)
    println(email)

  }

}

object Load {
  import searchengine.SearchEngine._
  import lookitup._

  // def loadDB(DB: DriverManagerTransactor[IO]): Unit = { //LookItUp = {
  //   println(DB)
  // }

}
