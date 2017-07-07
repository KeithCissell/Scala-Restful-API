// src/test/scala/SearchEngineSpecs.scala
import lookitup.LookItUp
import httpclient.HttpClient._
import httpclient.DuckDuckGoAPI._
import org.specs2.specification._
import searchengine.SearchEngine._
import org.specs2.mutable.Specification
import scala.collection.mutable.{ArrayBuffer => AB}

object SearchEngineSpecs extends Specification {
  /*******************************************************
  ** Create data to test with
  *******************************************************/
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
  val allUsers = new UserGroup(AB(Keith, Patrick, Lewis, Connor))
  val emptyGroup = new UserGroup()

  // Define Get/Post testing data
  val getTestURL = "https://httpbin.org/get"
  val postTestURL = "https://httpbin.org/post"
  val postMap = Map("message" -> "hello", "from" -> "keith", "to" -> "world")

  // Create HttpClient
  class TestClient extends HttpClient
  val testClient = new TestClient

  // Create SearchEngines
  val unpopularSearchEngine = new SearchEngine("Unpopular Engine", new UserGroup(AB(Lewis, Connor)))
  val smallSearchEngine = new SearchEngine("Small Engine", new UserGroup(AB(ConnorUpdate)))
  val popularSearchEngine = new SearchEngine("Popular Engine", allUsers)

  // Create LookItUp Engine
  val LookItUp = new LookItUp(new UserGroup(AB(Lewis)))


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
    "Return a ArrayBuffer of all Search elements" in {
      Keith.searchHistory.getAll == AB(weatherSearch, cardinalsSearch, cardinalsSearch)
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
      (Lewis.mostFrequentSearch === "No Search History") &&
      (Keith.mostFrequentSearch === "Cardinals")
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
    "Return a ArrayBuffer of all User elements" in {
      allUsers.getAll == AB(Keith, Patrick, Lewis, Connor)
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
      (unpopularSearchEngine.mostFrequentSearch === "No Searches Found") &&
      (popularSearchEngine.mostFrequentSearch === "Cardinals")
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

  // Look It Up Tests
  "\nLookItUp extends searcha engine with a DuckDuckGo API and" should {

    step(LookItUp.userSearch(Lewis.name, "test"))
    "userSearch makes a search on DDG and adds it to the user's history" in {
      !LookItUp.engineSearchHistory.isEmpty
    }
  }
}
