// src/main/scala/modulework/Http-Client/DuckDuckGoAPI.scala
package httpclient

import org.asynchttpclient._
import httpclient.HttpClient._
import searchengine.SearchEngine._
import org.json4s._
import org.json4s.jackson.JsonMethods._
import scala.util.matching.Regex
import scala.collection.mutable.{ArrayBuffer => AB}


object DuckDuckGoAPI {

  trait DuckDuckGoAPI extends HttpClient {

    // Make a search through DuckDuckGo's Instant Response API
    def searchDDG(query: String): Search = {
      val requestBody = Map("q" -> s"$query", "format" -> "json")
      val response = executeHttpPost("https://duckduckgo.com", requestBody)
      val results = extractResults(response.body).to[AB]
      Search(query, results)
    }

    // Overide POST execution
    override def executeHttpPost(url: String, body: Map[String,String]): HttpResponse = {
      val asyncHttpClient = new DefaultAsyncHttpClient()
      val request = asyncHttpClient.preparePost(s"$url")
      for ((n,v) <- body) request.addFormParam(n, v)
      val response = request.execute().get()
      asyncHttpClient.close()
      return formatResponse(response)
    }

    // Extract results from the returned Json data
    def extractResults(json: String): Seq[Result] = {
      val obj = parse(json)
      for {
        JObject(field) <- obj
        JField("Result", JString(result)) <- field
      } yield formatResult(result)
    }

    // Format result string into proper Result instance
    def formatResult(result: String): Result = {
      val split = result.split(">")
      val title = split(1).dropRight(3)
      val descirption = split(2)
      Result(title, descirption)
    }
  }

}
