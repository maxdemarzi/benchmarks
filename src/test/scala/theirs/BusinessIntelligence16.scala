package theirs

import io.gatling.core.Predef._
import io.gatling.core.scenario.Simulation
import io.gatling.http.Predef._

class BusinessIntelligence16 extends Simulation {
  // Setup server, username and password
  val httpProtocol = http
    .baseUrl("http://localhost:7474")
    .acceptHeader("application/json")
    .basicAuth("neo4j", "benchmark")

  val query =
    """MATCH
        (:Person {id: $personId})-[:KNOWS*3..5]-(person:Person)
      WITH DISTINCT person
      MATCH
        (person)-[:IS_LOCATED_IN]->(:City)-[:IS_PART_OF]->(:Country {name: $country}),
        (person)<-[:HAS_CREATOR]-(message:Message)-[:HAS_TAG]->(:Tag)-[:HAS_TYPE]->
        (:TagClass {name: $tagClass})
      MATCH
        (message)-[:HAS_TAG]->(tag:Tag)
      RETURN
        person.id,
        tag.name,
        count(DISTINCT message) AS messageCount
      ORDER BY
        messageCount DESC,
        tag.name ASC,
        person.id ASC
      LIMIT 100
    """.stripMargin.replaceAll("\n", " ")

  val statements = """{"statements" : [{"statement" : "%s", "parameters" : { "personId": 19791209310731, "country": "Pakistan", "tagClass": "MusicalArtist" } }] }"""
    .format(query)

  val scn = scenario("theirs.BusinessIntelligence16")
    .during(30 ) {
        exec(
          http("BI-16 (theirs)")
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