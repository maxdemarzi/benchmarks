package theirs

import io.gatling.core.Predef._
import io.gatling.core.scenario.Simulation
import io.gatling.http.Predef._

class BusinessIntelligence15 extends Simulation {
  // Setup server, username and password
  val httpProtocol = http
    .baseUrl("http://localhost:7474")
    .acceptHeader("application/json")
    .basicAuth("neo4j", "benchmark")

  val query =
    """MATCH
        (country:Country {name: $country})
      MATCH
        (country)<-[:IS_PART_OF]-(:City)<-[:IS_LOCATED_IN]-(person1:Person)
      OPTIONAL MATCH
        // start a new MATCH as friend might live in the same City
        // and thus can reuse the IS_PART_OF edge
        (country)<-[:IS_PART_OF]-(:City)<-[:IS_LOCATED_IN]-(friend1:Person),
        (person1)-[:KNOWS]-(friend1)
      WITH country, person1, count(friend1) AS friend1Count
      WITH country, avg(friend1Count) AS socialNormalFloat
      WITH country, floor(socialNormalFloat) AS socialNormal
      MATCH
        (country)<-[:IS_PART_OF]-(:City)<-[:IS_LOCATED_IN]-(person2:Person)
      OPTIONAL MATCH
        (country)<-[:IS_PART_OF]-(:City)<-[:IS_LOCATED_IN]-(friend2:Person)-[:KNOWS]-(person2)
      WITH country, person2, count(friend2) AS friend2Count, socialNormal
      WHERE friend2Count = socialNormal
      RETURN
        person2.id,
        friend2Count AS count
      ORDER BY
        person2.id ASC
      LIMIT 100
    """.stripMargin.replaceAll("\n", " ")

  val statements = """{"statements" : [{"statement" : "%s", "parameters" : { "country": "Burma" } }] }"""
    .format(query)

  val scn = scenario("theirs.BusinessIntelligence15")
    .during(30 ) {
        exec(
          http("BI-15 (theirs)")
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