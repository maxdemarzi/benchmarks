package theirs

import io.gatling.core.Predef._
import io.gatling.core.scenario.Simulation
import io.gatling.http.Predef._

class BusinessIntelligence19 extends Simulation {
  // Setup server, username and password
  val httpProtocol = http
    .baseUrl("http://localhost:7474")
    .acceptHeader("application/json")
    .basicAuth("neo4j", "benchmark")

  val query =
    """MATCH
        (:TagClass {name: $tagClass1})<-[:HAS_TYPE]-(:Tag)<-[:HAS_TAG]-
        (forum1:Forum)-[:HAS_MEMBER]->(stranger:Person)
      WITH DISTINCT stranger
      MATCH
        (:TagClass {name: $tagClass2})<-[:HAS_TYPE]-(:Tag)<-[:HAS_TAG]-
        (forum2:Forum)-[:HAS_MEMBER]->(stranger)
      WITH DISTINCT stranger
      MATCH
        (person:Person)<-[:HAS_CREATOR]-(comment:Comment)-[:REPLY_OF*]->(message:Message)-[:HAS_CREATOR]->(stranger)
      WHERE person.birthday > $date
        AND person <> stranger
        AND NOT (person)-[:KNOWS]-(stranger)
        AND NOT (message)-[:REPLY_OF*]->(:Message)-[:HAS_CREATOR]->(stranger)
      RETURN
        person.id,
        count(DISTINCT stranger) AS strangersCount,
        count(comment) AS interactionCount
      ORDER BY
        interactionCount DESC,
        person.id ASC
      LIMIT 100
    """.stripMargin.replaceAll("\n", " ")

  val statements = """{"statements" : [{"statement" : "%s", "parameters" : { "date": 19890101, "tagClass1": "MusicalArtist", "tagClass2": "OfficeHolder" } }] }"""
    .format(query)

  val scn = scenario("theirs.BusinessIntelligence19")
    .during(30 ) {
        exec(
          http("BI-19 (theirs)")
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