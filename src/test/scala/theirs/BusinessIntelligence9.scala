package theirs

import io.gatling.core.Predef._
import io.gatling.core.scenario.Simulation
import io.gatling.http.Predef._

class BusinessIntelligence9 extends Simulation {
  // Setup server, username and password
  val httpProtocol = http
    .baseUrl("http://localhost:7474")
    .acceptHeader("application/json")
    .basicAuth("neo4j", "benchmark")

  val query =
    """MATCH
        (forum:Forum)-[:HAS_MEMBER]->(person:Person)
      WITH forum, count(person) AS members
      WHERE members > $threshold
      MATCH
        (forum)-[:CONTAINER_OF]->(post1:Post)-[:HAS_TAG]->
        (:Tag)-[:HAS_TYPE]->(:TagClass {name: $tagClass1})
      WITH forum, count(DISTINCT post1) AS count1
      MATCH
        (forum)-[:CONTAINER_OF]->(post2:Post)-[:HAS_TAG]->
        (:Tag)-[:HAS_TYPE]->(:TagClass {name: $tagClass2})
      WITH forum, count1, count(DISTINCT post2) AS count2
      RETURN
        forum.id,
        count1,
        count2
      ORDER BY
        abs(count2-count1) DESC,
        forum.id ASC
      LIMIT 100
      
    """.stripMargin.replaceAll("\n", " ")

  val statements = """{"statements" : [{"statement" : "%s", "parameters" : { "tagClass1": "BaseballPlayer", "tagClass2": "ChristianBishop"," threshold": 200} }] }"""
    .format(query)

  val scn = scenario("theirs.BusinessIntelligence9")
    .during(30 ) {
        exec(
          http("BI-9 (theirs)")
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