package theirs

import io.gatling.core.Predef._
import io.gatling.core.scenario.Simulation
import io.gatling.http.Predef._

class BusinessIntelligence25 extends Simulation {
  // Setup server, username and password
  val httpProtocol = http
    .baseUrl("http://localhost:7474")
    .acceptHeader("application/json")
    .basicAuth("neo4j", "benchmark")

  val query =
    """MATCH
        path=allShortestPaths((p1:Person {id: $person1Id})-[:KNOWS*]-(p2:Person {id: $person2Id}))
      UNWIND relationships(path) AS k
      WITH
        path,
        startNode(k) AS pA,
        endNode(k) AS pB,
        0 AS relationshipWeights
      
      // case 1, A to B
      // every reply (by one of the Persons) to a Post (by the other Person): 1.0
      OPTIONAL MATCH
        (pA)<-[:HAS_CREATOR]-(c:Comment)-[:REPLY_OF]->(post:Post)-[:HAS_CREATOR]->(pB),
        (post)<-[:CONTAINER_OF]-(forum:Forum)
      WHERE forum.creationDate >= $startDate AND forum.creationDate <= $endDate
      WITH path, pA, pB, relationshipWeights + count(c)*1.0 AS relationshipWeights
      
      // case 2, A to B
      // every reply (by ones of the Persons) to a Comment (by the other Person): 0.5
      OPTIONAL MATCH
        (pA)<-[:HAS_CREATOR]-(c1:Comment)-[:REPLY_OF]->(c2:Comment)-[:HAS_CREATOR]->(pB),
        (c2)-[:REPLY_OF*]->(:Post)<-[:CONTAINER_OF]-(forum:Forum)
      WHERE forum.creationDate >= $startDate AND forum.creationDate <= $endDate
      WITH path, pA, pB, relationshipWeights + count(c1)*0.5 AS relationshipWeights
      
      // case 1, B to A
      // every reply (by one of the Persons) to a Post (by the other Person): 1.0
      OPTIONAL MATCH
        (pB)<-[:HAS_CREATOR]-(c:Comment)-[:REPLY_OF]->(post:Post)-[:HAS_CREATOR]->(pA),
        (post)<-[:CONTAINER_OF]-(forum:Forum)
      WHERE forum.creationDate >= $startDate AND forum.creationDate <= $endDate
      WITH path, pA, pB, relationshipWeights + count(c)*1.0 AS relationshipWeights
      
      // case 2, B to A
      // every reply (by ones of the Persons) to a Comment (by the other Person): 0.5
      OPTIONAL MATCH
        (pB)<-[:HAS_CREATOR]-(c1:Comment)-[:REPLY_OF]->(c2:Comment)-[:HAS_CREATOR]->(pA),
        (c2)-[:REPLY_OF*]->(:Post)<-[:CONTAINER_OF]-(forum:Forum)
      WHERE forum.creationDate >= $startDate AND forum.creationDate <= $endDate
      WITH path, pA, pB, relationshipWeights + count(c1)*0.5 AS relationshipWeights
      
      WITH
        [person IN nodes(path)  person.id] AS personIds,
        sum(relationshipWeights) AS weight
      
      RETURN
        personIds,
        weight
      ORDER BY
        weight DESC,
        personIds ASC
    """.stripMargin.replaceAll("\n", " ")

  val statements = """{"statements" : [{"statement" : "%s", "parameters" : { "person1Id": 19791209303405, "person2Id": 19791209308983, "startDate": 20101031230000000, "endDate": 20101130230000000 } }] }"""
    .format(query)

  val scn = scenario("theirs.BusinessIntelligence25")
    .during(30 ) {
        exec(
          http("BI-25 (theirs)")
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