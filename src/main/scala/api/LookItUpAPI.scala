// src/main/scala/api/LookItUpAPI.scala
package httpclient

import httpclient.HttpClient._
import searchengine.SearchEngine._
import org.json4s._
import org.json4s.jackson.JsonMethods._
import scala.util.matching.Regex
import scala.collection.mutable.{ArrayBuffer => AB}

object LookItUpAPI {

  trait LookItUpAPI extends HttpClient {

    val hostRoot = "http://localhost:8080"

    def ping: Unit = {
      val reqURL = hostRoot + "/ping"
      val resp = executeHttpGet(reqURL)
      // Handle response
      val message = resp.statusCode match {
        case 200  => "Pong"
        case _    => s"ERROR. Status Code: ${resp.statusCode}"
      }
      println(message)
    }

    def createUser(username: String, password: String): Unit = {
      val reqURL = hostRoot + "/create_user"
      val reqBody = Map("username" -> username, "password" -> password)
      val resp = executeHttpPost(reqURL, reqBody)
      // Handle response
      val message = resp.statusCode match {
        case 200  => s"Successfully added user: $username"
        case 403  => s"UserName, $username, already exists. Try another."
        case _    => s"ERROR. Status Code: ${resp.statusCode}"
      }
      println(message)
    }

    def changePassword(username: String, oldP: String, newP: String): Unit = {
      val reqURL = hostRoot + "/change_password"
      val reqBody = Map("username" -> username, "oldPassword" -> oldP, "newPassword" -> newP)
      val resp = executeHttpPost(reqURL, reqBody)
      // Handle response
      val message = resp.statusCode match {
        case 200  => s"Successfully changed password for: $username"
        case 403  => s"SOMETHING WENT WRONG"
        case _    => s"ERROR. Status Code: ${resp.statusCode}"
      }
      println(message)
    }

    def search(username: String, password: String, query: String): Unit = {
      val reqURL = hostRoot + "/search?q=:" + query
      val reqBody = Map("username" -> username, "password" -> password)
      val resp = executeHttpPost(reqURL, reqBody)
      // Handle response
      val message = resp.statusCode match {
        case 200  => s"${resp.body}"
        case 403  => s"Invalid username/password combo"
        case _    => s"ERROR. Status Code: ${resp.statusCode}"
      }
      println(message)
    }

    def getAllSearches: Unit = {
      val reqURL = hostRoot + "/search_terms"
      val resp = executeHttpGet(reqURL)
      // Handle response
      val message = resp.statusCode match {
        case 200  => s"${resp.body}"
        case _    => s"ERROR. Status Code: ${resp.statusCode}"
      }
      println(message)
    }

    def getUserSearches(username: String, password: String): Unit = {
      val reqURL = hostRoot + "/search_terms"
      val reqBody = Map("username" -> username, "password" -> password)
      val resp = executeHttpPost(reqURL, reqBody)
      // Handle response
      val message = resp.statusCode match {
        case 200  => s"${resp.body}"
        case 403  => s"Invalid username/password combo"
        case _    => s"ERROR. Status Code: ${resp.statusCode}"
      }
      println(message)
    }

    def mostCommonSearch: Unit = {
      val reqURL = hostRoot + "/most_common_search"
      val resp = executeHttpGet(reqURL)
      // Handle response
      val message = resp.statusCode match {
        case 200  => s"${resp.body}"
        case _    => s"ERROR. Status Code: ${resp.statusCode}"
      }
      println(message)
    }

    def userMostCommonSearch(username: String, password: String): Unit = {
      val reqURL = hostRoot + "/most_common_search"
      val reqBody = Map("username" -> username, "password" -> password)
      val resp = executeHttpPost(reqURL, reqBody)
      // Handle response
      val message = resp.statusCode match {
        case 200  => s"${resp.body}"
        case 403  => s"Invalid username/password combo"
        case _    => s"ERROR. Status Code: ${resp.statusCode}"
      }
      println(message)
    }

  }

}
