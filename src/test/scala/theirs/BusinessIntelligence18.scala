package theirs

import io.gatling.core.Predef._
import io.gatling.core.scenario.Simulation
import io.gatling.http.Predef._

class BusinessIntelligence18 extends Simulation {
  // Setup server, username and password
  val httpProtocol = http
    .baseUrl("http://localhost:7474")
    .acceptHeader("application/json")
    .basicAuth("neo4j", "benchmark")

  val query =
    """MATCH (person:Person)
      OPTIONAL MATCH (person)<-[:HAS_CREATOR]-(message:Message)-[:REPLY_OF*0..]->(post:Post)
      WHERE message.content IS NOT NULL
        AND message.length < $lengthThreshold
        AND message.creationDate > $date
        AND post.language IN $languages
      WITH
        person,
        count(message) AS messageCount
      RETURN
        messageCount,
        count(person) AS personCount
      ORDER BY
        personCount DESC,
        messageCount DESC
    """.stripMargin.replaceAll("\n", " ")

  val statements = """{"statements" : [{"statement" : "%s", "parameters" : { "date": 20110722000000000, "lengthThreshold": 20, "languages": ["ar"] } }] }"""
    .format(query)

  val scn = scenario("theirs.BusinessIntelligence18")
    .during(30 ) {
        exec(
          http("BI-18 (theirs)")
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