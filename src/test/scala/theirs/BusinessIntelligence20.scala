package theirs

import io.gatling.core.Predef._
import io.gatling.core.scenario.Simulation
import io.gatling.http.Predef._

class BusinessIntelligence20 extends Simulation {
  // Setup server, username and password
  val httpProtocol = http
    .baseUrl("http://localhost:7474")
    .acceptHeader("application/json")
    .basicAuth("neo4j", "benchmark")

  val query =
    """UNWIND $tagClasses AS tagClassName
      MATCH
        (tagClass:TagClass {name: tagClassName})<-[:IS_SUBCLASS_OF*0..]-
        (:TagClass)<-[:HAS_TYPE]-(tag:Tag)<-[:HAS_TAG]-(message:Message)
      RETURN
        tagClass.name,
        count(DISTINCT message) AS messageCount
      ORDER BY
        messageCount DESC,
        tagClass.name ASC
      LIMIT 100
    """.stripMargin.replaceAll("\n", " ")

  val statements = """{"statements" : [{"statement" : "%s", "parameters" : { "tagClasses": ["Writer", "Single", "Country"] } }] }"""
    .format(query)

  val scn = scenario("theirs.BusinessIntelligence20")
    .during(30 ) {
        exec(
          http("BI-20 (theirs)")
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