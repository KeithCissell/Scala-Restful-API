// src/main/scala/modulework/HTTP-Client/HttpClient.scala
package httpclient
import org.asynchttpclient._
import java.util.concurrent.Future
import org.json4s.jackson.Serialization
import scala.collection.JavaConversions._

object HttpClient{

  implicit val formats = org.json4s.DefaultFormats

  case class HttpResponse(header: Map[String,String], body: String, statusCode: Int)

  trait HttpClient {

    def executeHttpPost(url: String, body: Map[String,String]): HttpResponse = {
      val jsonBody = Serialization.write(body)
      val asyncHttpClient = new DefaultAsyncHttpClient()
      val request = asyncHttpClient.preparePost(url)
      request.setHeader("Content-Type", "application/json")
      request.setBody(jsonBody)
      val response = request.execute().get()
      asyncHttpClient.close()
      return formatResponse(response)
    }

    def executeHttpGet(url: String): HttpResponse = {
      val asyncHttpClient = new DefaultAsyncHttpClient()
      val request = asyncHttpClient.prepareGet(url)
      val response = request.execute().get()
      asyncHttpClient.close()
      return formatResponse(response)
    }

    def formatResponse(r: Response): HttpResponse = {
      val hMap: Map[String, String] = if (r.hasResponseHeaders()) {
        val names = r.getHeaders().names().toList
        val values = names.map(n => r.getHeaders().get(n))
        (names zip values).toMap
      } else Map()
      val b = if (r.hasResponseBody()) r.getResponseBody() else ""
      val s = if (r.hasResponseStatus()) r.getStatusCode() else 0
      HttpResponse(hMap, b, s)
    }
  }

}
