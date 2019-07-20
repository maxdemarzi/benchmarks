package better

import io.gatling.core.Predef._
import io.gatling.core.scenario.Simulation
import io.gatling.http.Predef._

class InteractiveComplex4 extends Simulation {
  // Setup server, username and password
  val httpProtocol = http
    .baseUrl("http://localhost:7474")
    .acceptHeader("application/json")
    .basicAuth("neo4j", "benchmark")

  val query =
    """MATCH (person:Person {id:$personId})-[:KNOWS]-(friend),
       (friend)<-[:HAS_CREATOR]-(post)-[:HAS_TAG]->(tag)
      WITH DISTINCT tag, post
      WITH tag,
      CASE
        WHEN ($startDate + $duration) > post.creationDate >= $startDate THEN 1
        ELSE 0
      END AS valid,
      CASE
        WHEN $startDate > post.creationDate THEN 1
        ELSE 0
      END AS inValid
      WITH tag, sum(valid) AS postCount, sum(inValid) AS inValidPostCount
      WHERE postCount>0 AND inValidPostCount=0
      RETURN tag.name AS tagName, postCount
      ORDER BY postCount DESC, tagName ASC
      LIMIT 10
    """.stripMargin.replaceAll("\n", " ")

  val statements = """{"statements" : [{"statement" : "%s", "parameters" : { "personId": 21990232559429, "startDate": 20120501000000000, "duration": 37000000000} }] }"""
    .format(query)

  val scn = scenario("better.InteractiveComplex4")
    .during(30 ) {
        exec(
          http("IC-4 (better)")
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