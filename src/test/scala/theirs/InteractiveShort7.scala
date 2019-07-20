package theirs

import io.gatling.core.Predef._
import io.gatling.core.scenario.Simulation
import io.gatling.http.Predef._

class InteractiveShort7 extends Simulation {
  // Setup server, username and password
  val httpProtocol = http
    .baseUrl("http://localhost:7474")
    .acceptHeader("application/json")
    .basicAuth("neo4j", "benchmark")

  val query =
    """MATCH (m:Message {id:$messageId})<-[:REPLY_OF]-(c:Comment)-[:HAS_CREATOR]->(p:Person)
      OPTIONAL MATCH (m)-[:HAS_CREATOR]->(a:Person)-[r:KNOWS]-(p)
      RETURN
        c.id AS commentId,
        c.content AS commentContent,
        c.creationDate AS commentCreationDate,
        p.id AS replyAuthorId,
        p.firstName AS replyAuthorFirstName,
        p.lastName AS replyAuthorLastName,
        CASE r
          WHEN null THEN false
          ELSE true
        END AS replyAuthorKnowsOriginalMessageAuthor
      ORDER BY commentCreationDate DESC, replyAuthorId
    """.stripMargin.replaceAll("\n", " ")

  val statements = """{"statements" : [{"statement" : "%s", "parameters" : { "messageId": 8246337208329 } }] }"""
    .format(query)

  val scn = scenario("theirs.InteractiveShort7")
    .during(30 ) {
        exec(
          http("IS-7 (theirs)")
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