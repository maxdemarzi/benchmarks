package better

import io.gatling.core.Predef._
import io.gatling.core.scenario.Simulation
import io.gatling.http.Predef._

class InteractiveComplex13 extends Simulation {
  // Setup server, username and password
  val httpProtocol = http
    .baseUrl("http://localhost:7474")
    .acceptHeader("application/json")
    .basicAuth("neo4j", "benchmark")

  val query =
    """MATCH (person1:Person {id:$person1Id}), (person2:Person {id:$person2Id})
       ,path = shortestPath((person1)-[:KNOWS*0..]-(person2))
       RETURN COALESCE(length(path), -1) AS pathLength
    """.stripMargin.replaceAll("\n", " ")

  val statements = """{"statements" : [{"statement" : "%s", "parameters" : { "person1Id": 8796093030404, "person2Id": 26388279074461} }] }"""
    .format(query)

  val scn = scenario("better.InteractiveComplex13")
    .during(30 ) {
        exec(
          http("IC-13 (better)")
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