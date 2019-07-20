package better

import io.gatling.core.Predef._
import io.gatling.core.scenario.Simulation
import io.gatling.http.Predef._

class InteractiveComplex10 extends Simulation {
  // Setup server, username and password
  val httpProtocol = http
    .baseUrl("http://localhost:7474")
    .acceptHeader("application/json")
    .basicAuth("neo4j", "benchmark")

  val query =
    """PROFILE MATCH (person:Person {id:$personId})-[:KNOWS*2..2]-(friend)-[:IS_LOCATED_IN]->(city)
       WHERE 
         ((friend.birthday/100%100 = $month AND friend.birthday%100 >= 21) OR
         (friend.birthday/100%100 = (($month % 12) + 1) AND friend.birthday%100 < 22))
         AND not(friend=person)
         AND not((friend)-[:KNOWS]-(person))
       WITH DISTINCT friend, city, person
       OPTIONAL MATCH (friend)<-[:HAS_CREATOR]-(post:Post)
       WITH friend, city, collect(post) AS posts, person
       WITH 
         friend,
         city,
         length(posts) AS postCount,
         length([p IN posts WHERE (p)-[:HAS_TAG]->()<-[:HAS_INTEREST]-(person)]) AS commonPostCount
       RETURN
         friend.id AS personId,
         friend.firstName AS personFirstName,
         friend.lastName AS personLastName,
         commonPostCount - (postCount - commonPostCount) AS commonInterestScore,
         friend.gender AS personGender,
         city.name AS personCityName
       ORDER BY commonInterestScore DESC, personId ASC
       LIMIT 10
    """.stripMargin.replaceAll("\n", " ")

  val statements = """{"statements" : [{"statement" : "%s", "parameters" : { "personId": 30786325583618, "month": 11} }] }"""
    .format(query)

  val scn = scenario("better.InteractiveComplex10")
    .during(30 ) {
        exec(
          http("IC-10 (better)")
            .post("/db/data/transaction/commit")
            .body(StringBody(statements))
            .asJson
            .check(status.is(200))
        )
    }

  setUp(scn.inject(atOnceUsers(8))).protocols(httpProtocol)

}

/*
    If you want to see the response from the server, add the following to the .check
            .check(bodyString.saveAs("BODY")))
            .exec(session => {
              val response = session("BODY").as[String]
              println(s"Response body: \n$response")
              session
            }
 */