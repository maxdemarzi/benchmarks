package better

import io.gatling.core.Predef._
import io.gatling.core.scenario.Simulation
import io.gatling.http.Predef._

class InteractiveComplex3Inverted extends Simulation {
  // Setup server, username and password
  val httpProtocol = http
    .baseUrl("http://localhost:7474")
    .acceptHeader("application/json")
    .basicAuth("neo4j", "benchmark")

  val query =
    """MATCH (countryX:Place {name:$countryXName}), (countryY:Place {name:$countryYName})
      MATCH (person:Person {id:$personId})-[:KNOWS*1..2]-(friend)
      WITH DISTINCT countryX, countryY, friend
      WHERE NOT (friend)-[:IS_LOCATED_IN]->()-[:IS_PART_OF]->(countryX)
        AND NOT (friend)-[:IS_LOCATED_IN]->()-[:IS_PART_OF]->(countryY)
      WITH countryX, countryY, collect(DISTINCT friend) as friends
      MATCH (friend)<-[:HAS_CREATOR]-(message)-[:IS_LOCATED_IN]->(country:Place)
      WHERE country.name in [$countryXName, $countryYName]
      AND friend in friends
      WITH countryX, countryY, friend, message, country, 1 as ignored
      WHERE $startDate <= message.creationDate < ($startDate + $duration)
      WITH countryX, countryY, country, friend, count(message) as count
      WITH friend, CASE WHEN country = countryX THEN count ELSE 0 END as countX, CASE WHEN country = countryY THEN count ELSE 0 END as countY
      WITH friend, sum(countX) as xCount, sum(countY) as yCount
      WHERE xCount <> 0 AND yCount <> 0
      WITH friend, xCount, yCount, xCount + yCount as count
      ORDER BY count DESC, toInteger(friend.id) ASC
      LIMIT 20
      RETURN  friend.id AS personId,
              friend.firstName AS personFirstName,
              friend.lastName AS personLastName,
              xCount,
              yCount,
              count
    """.stripMargin.replaceAll("\n", " ")

  val statements = """{"statements" : [{"statement" : "%s", "parameters" : { "personId": 17592186055119, "startDate": 20120501000000000, "duration":42000000000, "countryXName":'Laos', "countryYName":'Scotland' } }] }"""
    .format(query)

  val scn = scenario("better.InteractiveComplex3Inverted")
    .during(30 ) {
        exec(
          http("IC-3 (better)")
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