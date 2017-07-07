# Scala-Restful-API
"Look It Up" is a restful search API using DuckDuckGo's instant response API to fill scala data structures with results. "Look It Up" uses a server-side and client-side API to allow server storage of data, accessed through HTTP communication.

## Server
The Look It Up (LIU) server binds to `http://localhost:8080` by default and uses `LIUService.scala` to handle any incoming requests.

### Running the Server
Open a command window within the root folder and run sbt.

`sbt run`

If sbt detects multiple main services, select `lookitup.server.LIUServer`.

### Services
The following is a list of URIs that `LIUService.scala` will handle.

_NOTE: The LookItUpAPI is an easy to use front-end API that will properly format requests and responses to/from the LIUServer._

```
ROUTE                     METHOD    DESCRIPTION
/ping                     GET       Returns "Pong"
/create_user              POST      Creates a new user if username doesn't exist in database already
/change_password          POST      Changes existing user's password
/search?q=searchString    POST      Takes user credentials and searches "searchString" from the query for the user
/search_terms             GET       Returns all searches made on Look It UP
/search_terms             POST      Returns all searches made by given user
/most_common_search       GET       Returns most common searches made on Look It Up
/most_common_search       POST      Returns most common searches made by given user
```

### Requests
All `POST` methods require properly structured requests.

__Header__

"Content-Type"="application/json"

__Body__

All `POST` bodies only need the user's credentials passed in Json format, with the exception of `/change_password`.

Credentials:
```
{
  "username": "username",
  "password": "password"
}
```

`/change_password`:
```
{
  "username": "username",
  "oldPassword": "oldPass",
  "newPassword": "different!"
}
```

## Look It Up API
This API provides convenient methods that can be used to communicate with the LIUServer within scala. The `LookItUpPI` is a scala `trait` that can be mixed-in with custom classes. To run use this API, a LIUServer must already be properly be setup, and the `host` value in `LookItUpAPI` must be correctly set to the where the server is bound.

### Example Usage
```
class MySearchPage extends LookItUpAPI {
  override val host = "https://YourDomainHere.com"
}
```

### Methods
#### `ping`
__Parameters:__

_None_

__Description:__

Attempts to reach server. Server will respond with "Pong"

#### `createUser`
__Parameters:__
- `username: String`
- `password: String`

__Description:__

Creates new user if the username does not already exist in the database

#### `changePassword`
__Parameters:__
- `username: String`
- `oldP: String`
- `newP: String`

__Description:__

Updates users password if username/oldP are valid, and oldP is different than newP.

#### `search`
__Parameters:__
- `username: String`
- `password: String`
- `query: String`

__Description:__

If username/password are valid, makes a search for the user. Returns Json containing all results received when searching DuckDuckGo's Instant Answer API.

#### `getAllSearches`
__Parameters:__

_None_

__Description:__

Returns all searches that have been made on this Look It Up search engine.

#### `getUserSearches`
__Parameters:__
- `username: String`
- `password: String`

__Description:__

If username/password are valid, returns all searches made by the specified user.

#### `mostFrequentSearch`
__Parameters:__

_None_

__Description:__

Returns the most frequent search(es) made on the Look It Up search engine.

#### `userMostFrequentSearch`
__Parameters:__
- `username: String`
- `password: String`

__Description:__

If username/password are valid, returns the most frequent search(es) made by the specified user.

## Mock Run
`Scala-Search-Engine/src/main/scala/MockRun.scala`

This file contains mock data that is put in a `LookItUp` data structure as well as a simple instance of a `LookItUpAPI`. This can be used as a sandbox to test/play with the data structures and APIs in use.

_Note: If you are testing/playing with the LookItUpAPI, the LIUServer must be up and running. It can be started as described under "Running the Server" at the top of this document._

## Testing
A test suite is setup using specs2. It can be run from a command window in sbt using this command:

`sbt test`

## External Libraries Used
- [Async Http Client](https://github.com/AsyncHttpClient/async-http-client)
- [DuckDuckGo Instant Answer API](https://duckduckgo.com/api)
- [http4s](http://http4s.org/)
- [json4s](http://json4s.org/)
- [specs2](https://etorreborre.github.io/specs2/)
