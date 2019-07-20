package theirs

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
    """MATCH
        (person:Person {id:$personId})-[:KNOWS*1..2]-(friend:Person),
        (friend)<-[:HAS_CREATOR]-(friendPost:Post)-[:HAS_TAG]->(knownTag:Tag {name:$tagName})
      WHERE not(person=friend)
      MATCH (friendPost)-[:HAS_TAG]->(commonTag:Tag)
      WHERE not(commonTag=knownTag)
      WITH DISTINCT commonTag, knownTag, friend
      MATCH (commonTag)<-[:HAS_TAG]-(commonPost:Post)-[:HAS_TAG]->(knownTag)
      WHERE (commonPost)-[:HAS_CREATOR]->(friend)
      RETURN
        commonTag.name AS tagName,
        count(commonPost) AS postCount
      ORDER BY postCount DESC, tagName ASC
      LIMIT 10
    """.stripMargin.replaceAll("\n", " ")

  val statements = """{"statements" : [{"statement" : "%s", "parameters" : { "personId": 30786325583618, "tagName": "Angola"} }] }"""
    .format(query)

  val scn = scenario("theirs.InteractiveComplex6")
    .during(30 ) {
        exec(
          http("IC-6 (theirs)")
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