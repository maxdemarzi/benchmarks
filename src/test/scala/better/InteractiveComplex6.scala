package better

import io.gatling.core.Predef._
import io.gatling.core.scenario.Simulation
import io.gatling.http.Predef._

class InteractiveComplex6 extends Simulation {
  // Setup server, username and password
  val httpProtocol = http
    .baseUrl("http://localhost:7474")
    .acceptHeader("application/json")
    .basicAuth("neo4j", "benchmark")

  val query =
    """MATCH (commonTag)<-[:HAS_TAG]-(friendPost:Post)-[:HAS_TAG]->(knownTag:Tag {name:$tagName})
       WITH commonTag, friendPost
       MATCH (friendPost)-[:HAS_CREATOR]->(friend)-[:KNOWS*1..2]-(person:Person {id:$personId})
       WHERE friend <> person
       RETURN
         commonTag.name AS tagName,
         count(DISTINCT friendPost) AS postCount
       ORDER BY postCount DESC, tagName ASC
       LIMIT 10
    """.stripMargin.replaceAll("\n", " ")

  val statements = """{"statements" : [{"statement" : "%s", "parameters" : { "personId": 30786325583618, "tagName": "Angola"} }] }"""
    .format(query)

  val scn = scenario("better.InteractiveComplex6")
    .during(30 ) {
        exec(
          http("IC-6 (better)")
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