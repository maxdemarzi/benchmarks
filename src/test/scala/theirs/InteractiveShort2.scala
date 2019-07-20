package theirs

import io.gatling.core.Predef._
import io.gatling.core.scenario.Simulation
import io.gatling.http.Predef._

class InteractiveShort2 extends Simulation {
  // Setup server, username and password
  val httpProtocol = http
    .baseUrl("http://localhost:7474")
    .acceptHeader("application/json")
    .basicAuth("neo4j", "benchmark")

  val query =
    """MATCH (:Person {id:$personId})<-[:HAS_CREATOR]-(m:Message)-[:REPLY_OF*0..]->(p:Post)
      MATCH (p)-[:HAS_CREATOR]->(c)
      RETURN
        m.id as messageId,
        CASE exists(m.content)
          WHEN true THEN m.content
          ELSE m.imageFile
        END AS messageContent,
        m.creationDate AS messageCreationDate,
        p.id AS originalPostId,
        c.id AS originalPostAuthorId,
        c.firstName as originalPostAuthorFirstName,
        c.lastName as originalPostAuthorLastName
      ORDER BY messageCreationDate DESC
      LIMIT 10
    """.stripMargin.replaceAll("\n", " ")

  val statements = """{"statements" : [{"statement" : "%s", "parameters" : { "personId": 17592186053137 } }] }"""
    .format(query)

  val scn = scenario("theirs.InteractiveShort2")
    .during(30 ) {
        exec(
          http("IS-2 (theirs)")
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