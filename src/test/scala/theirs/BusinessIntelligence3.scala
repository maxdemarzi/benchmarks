package theirs

import io.gatling.core.Predef._
import io.gatling.core.scenario.Simulation
import io.gatling.http.Predef._

class BusinessIntelligence3 extends Simulation {
  // Setup server, username and password
  val httpProtocol = http
    .baseUrl("http://localhost:7474")
    .acceptHeader("application/json")
    .basicAuth("neo4j", "benchmark")

  val query =
    """WITH
        $year AS year1,
        $month AS month1,
        $year + toInteger($month / 12.0) AS year2,
        $month % 12 + 1 AS month2
      MATCH (tag:Tag)
      OPTIONAL MATCH (message1:Message)-[:HAS_TAG]->(tag)
        WHERE message1.creationDate/10000000000000   = year1
          AND message1.creationDate/100000000000%100 = month1
      WITH year2, month2, tag, count(message1) AS countMonth1
      OPTIONAL MATCH (message2:Message)-[:HAS_TAG]->(tag)
        WHERE message2.creationDate/10000000000000   = year2
          AND message2.creationDate/100000000000%100 = month2
      WITH
        tag,
        countMonth1,
        count(message2) AS countMonth2
      RETURN
        tag.name,
        countMonth1,
        countMonth2,
        abs(countMonth1-countMonth2) AS diff
      ORDER BY
        diff DESC,
        tag.name ASC
      LIMIT 100
    """.stripMargin.replaceAll("\n", " ")

  val statements = """{"statements" : [{"statement" : "%s", "parameters" : { "year": 2010, "month":  10 } }] }"""
    .format(query)

  val scn = scenario("theirs.BusinessIntelligence3")
    .during(30 ) {
        exec(
          http("BI-3 (theirs)")
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