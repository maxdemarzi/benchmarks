package theirs

import io.gatling.core.Predef._
import io.gatling.core.scenario.Simulation
import io.gatling.http.Predef._

class BusinessIntelligence14 extends Simulation {
  // Setup server, username and password
  val httpProtocol = http
    .baseUrl("http://localhost:7474")
    .acceptHeader("application/json")
    .basicAuth("neo4j", "benchmark")

  val query =
    """MATCH (person:Person)<-[:HAS_CREATOR]-(post:Post)<-[:REPLY_OF*0..]-(reply:Message)
      WHERE  post.creationDate >= $startDate
        AND  post.creationDate <= $endDate
        AND reply.creationDate >= $startDate
        AND reply.creationDate <= $endDate
      RETURN
        person.id,
        person.firstName,
        person.lastName,
        count(DISTINCT post) AS threadCount,
        count(DISTINCT reply) AS messageCount
      ORDER BY
        messageCount DESC,
        person.id ASC
      LIMIT 100
    """.stripMargin.replaceAll("\n", " ")

  val statements = """{"statements" : [{"statement" : "%s", "parameters" : { "startDate": 20120531220000000, "endDate": 20120630220000000 } }] }"""
    .format(query)

  val scn = scenario("theirs.BusinessIntelligence14")
    .during(30 ) {
        exec(
          http("BI-14 (theirs)")
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