package theirs

import io.gatling.core.Predef._
import io.gatling.core.scenario.Simulation
import io.gatling.http.Predef._

class BusinessIntelligence24 extends Simulation {
  // Setup server, username and password
  val httpProtocol = http
    .baseUrl("http://localhost:7474")
    .acceptHeader("application/json")
    .basicAuth("neo4j", "benchmark")

  val query =
    """MATCH (:TagClass {name: $tagClass})<-[:HAS_TYPE]-(:Tag)<-[:HAS_TAG]-(message:Message)
      WITH DISTINCT message
      MATCH (message)-[:IS_LOCATED_IN]->(:Country)-[:IS_PART_OF]->(continent:Continent)
      OPTIONAL MATCH (message)<-[like:LIKES]-(:Person)
      WITH
        message,
        message.creationDate/10000000000000   AS year,
        message.creationDate/100000000000%100 AS month,
        like,
        continent
      RETURN
        count(DISTINCT message) AS messageCount,
        count(like) AS likeCount,
        year,
        month,
        continent.name
      ORDER BY
        year ASC,
        month ASC,
        continent.name DESC
      LIMIT 100
    """.stripMargin.replaceAll("\n", " ")

  val statements = """{"statements" : [{"statement" : "%s", "parameters" : { "tagClass": "Single" } }] }"""
    .format(query)

  val scn = scenario("theirs.BusinessIntelligence24")
    .during(30 ) {
        exec(
          http("BI-24 (theirs)")
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