// src/test/scala/SearchEngineSpecs.scala
import lookitup.LookItUp
import searchengine.SearchEngine._
import httpclient.LookItUpAPI._
import httpclient.DuckDuckGoAPI._

import httpclient.HttpClient._
import org.specs2.specification._
import org.specs2.mutable.Specification
import akka.actor._
import akka.testkit._

import scala.collection.mutable.{ArrayBuffer => AB}

object SearchEngineSpecs extends Specification {
  /*******************************************************
  ** Create data to test with
  *******************************************************/
  // Create some Results
  val testResult = Result("Springfield's Weather", "Local weather report for your area.")

  // Make some searches and fill them with results
  val weatherSearch = Search("Weather", AB(
    Result("Springfield's Weather", "Local weather report for your area."),
    Result("National Weather Report", "Your up to date location for weather around the world."))
  )
  val cardinalsSearch = Search("Cardinals", AB(
    Result("Cardinals Nation", "You're one stop for up to date Cardinal's score and news"),
    Result("MLB Network", "Cardinals vs. Orioles: Live Score Updates"))
  )
  val testSearch = Search("test", AB(
    Result("test1", "this search is added, updated and then removed during testing"))
  )
  val testSearchUpdate = Search("test", AB(
    Result("test2", "this is used to check if update works"))
  )

  // Create Users
  val Keith = new User("Keith", "StrongPassWord", SearchHistory(AB(weatherSearch, cardinalsSearch, cardinalsSearch)))
  val Patrick = new User("Pat", "123456", SearchHistory(AB(cardinalsSearch, cardinalsSearch, weatherSearch)))
  val Lewis = new User("LewCustom", "K7L")
  val Connor = new User("Conair", "wordPass")
  val ConnorUpdate = new User("Conair", "newWordPass", SearchHistory(AB(weatherSearch)))

  // Create UserGroups
  val allUsers = new UserGroup(List(Keith, Patrick, Lewis, Connor))
  val emptyGroup = new UserGroup()

  // Define Get/Post testing data
  val getTestURL = "https://httpbin.org/get"
  val postTestURL = "https://httpbin.org/post"
  val postMap = Map("message" -> "hello", "from" -> "keith", "to" -> "world")

  // Create HttpClient
  class TestClient extends HttpClient
  val testClient = new TestClient

  // Create SearchEngines
  val unpopularSearchEngine = new SearchEngine("Unpopular Engine", List(Lewis, Connor))
  val smallSearchEngine = new SearchEngine("Small Engine", List(ConnorUpdate))
  val popularSearchEngine = new SearchEngine("Popular Engine", List(Keith, Patrick, Lewis, Connor))

  // Create LookItUp Engine
  val LookItUp = new LookItUp(List(Lewis))

  // Create LookItUpAPI
  class LIUAPI extends LookItUpAPI
  val LIU = new LIUAPI

  // Create vars to hold responses
  val emptyResponse = HttpResponse(Map.empty, "", 1)
  var createUserResponse = emptyResponse
  var changePasswordResponse = emptyResponse
  var userSearchResponse = emptyResponse

  // Akka Actors
  implicit val system = ActorSystem()


  /*******************************************************
  ** Specs2 Tests
  *******************************************************/

  // Search Tests
  "\nSearchHistory is a Repository of Searchs that" should {

    "Check if empty" in {
      (!Keith.searchHistory.isEmpty) && (Lewis.searchHistory.isEmpty)
    }
    "Check if history contains a Search" in {
      Keith.searchHistory.contains(cardinalsSearch)
    }
    "Return a List of all Search elements" in {
      Keith.searchHistory.getAll == List(weatherSearch, cardinalsSearch, cardinalsSearch)
    }
    "Get a Search at the indicated index" in {
      (Keith.searchHistory.get(2) == Some(cardinalsSearch)) && (Keith.searchHistory.get(4) == None)
    }
    step(Keith.searchHistory.create(testSearch))
    "Add a new Search to the history" in {
      Keith.searchHistory.getAll == AB(weatherSearch, cardinalsSearch, cardinalsSearch, testSearch)
    }
    step(Keith.searchHistory.update(testSearchUpdate))
    "Update Searches in the history" in {
      (!Keith.searchHistory.contains(testSearch)) && (Keith.searchHistory.contains(testSearchUpdate))
    }
    step(Keith.searchHistory.delete(testSearchUpdate))
    "Delete searches from the history" in {
      !Keith.searchHistory.contains(testSearchUpdate)
    }
  }

  // User Tests
  "\nUser holds an identity and searchHistory and" should {

    "Find the User's most frequent search" in {
      (Lewis.mostFrequentSearch === List.empty) &&
      (Keith.mostFrequentSearch === List("Cardinals"))
    }
    "Properly formats a string" in {
      (ConnorUpdate.toString == s"Conair's Search History\n${SearchHistory(AB(weatherSearch))}") &&
      (Connor.toString == "Conair's Search History\nEmpty")
    }
  }

  // UserGroup Tests
  "\nUserGroup is a Repository of Users that" should {

    "Check if empty" in {
      (!allUsers.isEmpty) && (emptyGroup.isEmpty)
    }
    "Check if group contains a User" in {
      allUsers.contains(Keith.name)
    }
    "Return a List of all Users" in {
      allUsers.getAll == List(Keith, Patrick, Lewis, Connor)
    }
    "Get a User by their name" in {
      (allUsers.get("Keith") == Some(Keith)) && (emptyGroup.get("Keith") == None)
    }
    step(emptyGroup.create(Connor))
    "Add a new User to the group" in {
      emptyGroup.getAll == AB(Connor)
    }
    step(emptyGroup.update(ConnorUpdate))
    "Update User in the group" in {
      (emptyGroup.getAll == AB(ConnorUpdate))
    }
    step(emptyGroup.delete(ConnorUpdate))
    "Delete User from the group" in {
      !emptyGroup.contains(ConnorUpdate.name)
    }
  }

  // SearchEngine Tests
  "\nSearchEngine holds a UserGroup that" should {

    "Return search history from all users" in {
      smallSearchEngine.engineSearchHistory == AB(weatherSearch)
    }
    "Find the SearchEngine's most frequent search" in {
      (unpopularSearchEngine.mostFrequentSearch === List.empty) &&
      (popularSearchEngine.mostFrequentSearch === List("Cardinals"))
    }
  }

  // HttpClient Tests
  "\nAPI allows use of HTTP Client functions that" should {

    "Successfully make a Get request" in {
      testClient.executeHttpGet(getTestURL).statusCode == 200
    }
    "Successfully make a Post request" in {
      testClient.executeHttpPost(postTestURL, postMap).statusCode == 200
    }
  }

  // LookItUp Tests
  "\nLookItUp SearchEngine" should {

    step(LookItUp.addSearchHistory(Lewis.name, testSearch))
    "Add a search to user's history" in {
      !LookItUp.engineSearchHistory.isEmpty
    }
  }

  // LookItUp Server Tests
  "\nLookItUpAPI is a client-API that" should {

    "Ping server" in {
      LIU.ping.statusCode == 200
    }
    step(createUserResponse = LIU.createUser("keith", "password"))
    "Create a new user" in {
      createUserResponse.statusCode == 200
    }
    step(changePasswordResponse = LIU.changePassword("keith", "password", "wordpass"))
    "Change a user's password" in {
      changePasswordResponse.statusCode == 200
    }
    step(userSearchResponse = LIU.search("keith", "wordpass", "Cardinals"))
    "Make a search for a user" in {
      userSearchResponse.statusCode == 200
    }
    "Return all searches made on the engine" in {
      LIU.getAllSearches.statusCode == 200
    }
    "Return all searches made by a given user" in {
      LIU.getUserSearches("keith", "wordpass").statusCode == 200
    }
    "Return the most frequent search on the engine" in {
      LIU.mostFrequentSearch.statusCode == 200
    }
    "Return the most frequent search made by a given user" in {
      LIU.userMostFrequentSearch("keith", "wordpass").statusCode == 200
    }
  }

  // Akka Actor Tests
  "\nLookItUp Actor" should {
    true
    // "Return list of search results" in {
    //   val probe = TestProbe()
    //   val liuActor = system.actorOf(LIUActor.props( new LookItUp ))
    //
    //   liu.tell(SearchActor.RequestResults, probe.ref)
    //   val response = probe.expectMsgType[SearchActor.RespondResults]
    //   response.results should ===(List.empty)
    // }
    //
    // "Record results" in {
    //   val probe = TestProbe()
    //   val searchActor = system.actorOf(SearchActor.props("Keith", "test"))
    //
    //   searchActor.tell(SearchActor.RecordResults(List(testResult)), probe.ref)
    //   searchActor.tell(SearchActor.RequestResults, probe.ref)
    //   val response = probe.expectMsgType[SearchActor.RespondResults]
    //   response.results should ===(List(testResult))
    // }
  }

}
