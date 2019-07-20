package theirs

import io.gatling.core.Predef._
import io.gatling.core.scenario.Simulation
import io.gatling.http.Predef._

class InteractiveShort1 extends Simulation {
  // Setup server, username and password
  val httpProtocol = http
    .baseUrl("http://localhost:7474")
    .acceptHeader("application/json")
    .basicAuth("neo4j", "benchmark")

  val query =
    """MATCH (n:Person {id:$personId})-[:IS_LOCATED_IN]->(p:Place)
      RETURN
        n.firstName AS firstName,
        n.lastName AS lastName,
        n.birthday AS birthday,
        n.locationIP AS locationIP,
        n.browserUsed AS browserUsed,
        p.id AS cityId,
        n.gender AS gender,
        n.creationDate AS creationDate
    """.stripMargin.replaceAll("\n", " ")

  val statements = """{"statements" : [{"statement" : "%s", "parameters" : { "personId": 17592186053137 } }] }"""
    .format(query)

  val scn = scenario("theirs.InteractiveShort1")
    .during(30 ) {
        exec(
          http("IS-1 (theirs)")
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