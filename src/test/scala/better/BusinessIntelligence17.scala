package better

import io.gatling.core.Predef._
import io.gatling.core.scenario.Simulation
import io.gatling.http.Predef._

class BusinessIntelligence17 extends Simulation {
  // Setup server, username and password
  val httpProtocol = http
    .baseUrl("http://localhost:7474")
    .acceptHeader("application/json")
    .basicAuth("neo4j", "benchmark")

  val query =
    """MATCH (country:Country {name: $country})
       WITH country
       MATCH (a:Person)-[:IS_LOCATED_IN]->(:City)-[:IS_PART_OF]->(country)
       WITH collect(distinct a) as aPrime
       WITH aPrime, aPrime as bPrime
       UNWIND aPrime as ap
       WITH ap,bPrime
       UNWIND bPrime as bp
       MATCH (ap)-[:KNOWS]-(bp)
       WHERE ap.id < bp.id
       WITH ap, bp
       match (bp)-[:KNOWS]-(c) where c.id < ap.id
       AND (c)-[:IS_LOCATED_IN]->(:City)-[:IS_PART_OF]->(:Country {name: $country})
       AND (c)-[:KNOWS]-(ap)
       RETURN count(*) AS count
    """.stripMargin.replaceAll("\n", " ")

  val statements = """{"statements" : [{"statement" : "%s", "parameters" : { "country": "Spain" } }] }"""
    .format(query)

  val scn = scenario("better.BusinessIntelligence17")
    .during(30 ) {
        exec(
          http("BI-17 (better)")
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