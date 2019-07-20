package theirs

import io.gatling.core.Predef._
import io.gatling.core.scenario.Simulation
import io.gatling.http.Predef._

class BusinessIntelligence13 extends Simulation {
  // Setup server, username and password
  val httpProtocol = http
    .baseUrl("http://localhost:7474")
    .acceptHeader("application/json")
    .basicAuth("neo4j", "benchmark")

  val query =
    """MATCH (:Country {name: $country})<-[:IS_LOCATED_IN]-(message:Message)
      OPTIONAL MATCH (message)-[:HAS_TAG]->(tag:Tag)
      WITH
        message.creationDate/10000000000000   AS year,
        message.creationDate/100000000000%100 AS month,
        message,
        tag
      WITH year, month, count(message) AS popularity, tag
      ORDER BY popularity DESC, tag.name ASC
      WITH
        year,
        month,
        collect([tag.name, popularity]) AS popularTags
      WITH
        year,
        month,
        [popularTag IN popularTags WHERE popularTag[0] IS NOT NULL] AS popularTags
      RETURN
        year,
        month,
        popularTags[0..5] AS topPopularTags
      ORDER BY
        year DESC,
        month ASC
      LIMIT 100
    """.stripMargin.replaceAll("\n", " ")

  val statements = """{"statements" : [{"statement" : "%s", "parameters" : { "country": "Burma" } }] }"""
    .format(query)

  val scn = scenario("theirs.BusinessIntelligence13")
    .during(30 ) {
        exec(
          http("BI-13 (theirs)")
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