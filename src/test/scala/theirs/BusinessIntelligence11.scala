package theirs

import io.gatling.core.Predef._
import io.gatling.core.scenario.Simulation
import io.gatling.http.Predef._

class BusinessIntelligence11 extends Simulation {
  // Setup server, username and password
  val httpProtocol = http
    .baseUrl("http://localhost:7474")
    .acceptHeader("application/json")
    .basicAuth("neo4j", "benchmark")

  val query =
    """WITH $blacklist AS blacklist
      MATCH
        (country:Country {name: $country})<-[:IS_PART_OF]-(:City)<-[:IS_LOCATED_IN]-
        (person:Person)<-[:HAS_CREATOR]-(reply:Comment)-[:REPLY_OF]->(message:Message),
        (reply)-[:HAS_TAG]->(tag:Tag)
      WHERE NOT (message)-[:HAS_TAG]->(:Tag)<-[:HAS_TAG]-(reply)
        AND size([word IN blacklist WHERE reply.content CONTAINS word  word]) = 0
      OPTIONAL MATCH
        (:Person)-[like:LIKES]->(reply)
      RETURN
        person.id,
        tag.name,
        count(DISTINCT like) AS countLikes,
        count(DISTINCT reply) AS countReplies
      ORDER BY
        countLikes DESC,
        person.id ASC,
        tag.name ASC
      LIMIT 100
    """.stripMargin.replaceAll("\n", " ")

  val statements = """{"statements" : [{"statement" : "%s", "parameters" : { "country": "Germany", "blacklist": ["also", "Pope", "that", "James", "Henry", "one", "Green"] } }] }"""
    .format(query)

  val scn = scenario("theirs.BusinessIntelligence11")
    .during(30 ) {
        exec(
          http("BI-11 (theirs)")
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