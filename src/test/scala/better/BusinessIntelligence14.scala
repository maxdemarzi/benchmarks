package better

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
    """MATCH (post:Post)
      WHERE  $endDate >= post.creationDate >= $startDate
      WITH post
      MATCH (post)<-[:REPLY_OF*0..]-(reply)
      WHERE $endDate >= reply.creationDate >= $startDate
      WITH post, reply
      MATCH (person)<-[:HAS_CREATOR]-(post)
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

  val scn = scenario("better.BusinessIntelligence14")
    .during(30 ) {
        exec(
          http("BI-14 (better)")
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