package better

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
    """MATCH (author)<-[:HAS_CREATOR]-(message:Message {id:$messageId}),
       (message)<-[:REPLY_OF]-(reply),
       (reply)-[:HAS_CREATOR]->(replyAuthor)
       RETURN
        replyAuthor.id AS replyAuthorId,
        replyAuthor.firstName AS replyAuthorFirstName,
        replyAuthor.lastName AS replyAuthorLastName,
        reply.id AS commentId,
        reply.content AS commentContent,
        reply.creationDate AS commentCreationDate,
        exists((author)-[:KNOWS]-(replyAuthor)) AS replyAuthorKnowsOriginalMessageAuthor
       ORDER BY commentCreationDate DESC, replyAuthorId ASC
    """.stripMargin.replaceAll("\n", " ")

  val statements = """{"statements" : [{"statement" : "%s", "parameters" : { "messageId": 8246337208329 } }] }"""
    .format(query)

  val scn = scenario("better.InteractiveShort7")
    .during(30 ) {
        exec(
          http("IS-7 (better)")
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