package better

import io.gatling.core.Predef._
import io.gatling.core.scenario.Simulation
import io.gatling.http.Predef._

class InteractiveShort6 extends Simulation {
  // Setup server, username and password
  val httpProtocol = http
    .baseUrl("http://localhost:7474")
    .acceptHeader("application/json")
    .basicAuth("neo4j", "benchmark")

  val query =
    """MATCH (m:Message {id:$messageId})-[:REPLY_OF*0..]->(p:Post)<-[:CONTAINER_OF]-(f)-[:HAS_MODERATOR]->(mod)
      RETURN
        f.id AS forumId,
        f.title AS forumTitle,
        mod.id AS moderatorId,
        mod.firstName AS moderatorFirstName,
        mod.lastName AS moderatorLastName
        LIMIT 1
    """.stripMargin.replaceAll("\n", " ")

  val statements = """{"statements" : [{"statement" : "%s", "parameters" : { "messageId": 4947802324994 } }] }"""
    .format(query)

  val scn = scenario("better.InteractiveShort6")
    .during(30 ) {
        exec(
          http("IS-6 (better)")
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