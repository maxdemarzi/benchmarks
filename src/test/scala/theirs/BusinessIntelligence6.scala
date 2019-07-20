package theirs

import io.gatling.core.Predef._
import io.gatling.core.scenario.Simulation
import io.gatling.http.Predef._

class BusinessIntelligence6 extends Simulation {
  // Setup server, username and password
  val httpProtocol = http
    .baseUrl("http://localhost:7474")
    .acceptHeader("application/json")
    .basicAuth("neo4j", "benchmark")

  val query =
    """MATCH (tag:Tag {name: $tag})<-[:HAS_TAG]-(message:Message)-[:HAS_CREATOR]->(person:Person)
      OPTIONAL MATCH (:Person)-[like:LIKES]->(message)
      OPTIONAL MATCH (message)<-[:REPLY_OF]-(comment:Comment)
      WITH person, count(DISTINCT like) AS likeCount, count(DISTINCT comment) AS replyCount, count(DISTINCT message) AS messageCount
      RETURN
        person.id,
        replyCount,
        likeCount,
        messageCount,
        1*messageCount + 2*replyCount + 10*likeCount AS score
      ORDER BY
        score DESC,
        person.id ASC
      LIMIT 100
    """.stripMargin.replaceAll("\n", " ")

  val statements = """{"statements" : [{"statement" : "%s", "parameters" : { "tag": "Abbas_I_of_Persia"} }] }"""
    .format(query)

  val scn = scenario("theirs.BusinessIntelligence6")
    .during(30 ) {
        exec(
          http("BI-6 (theirs)")
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