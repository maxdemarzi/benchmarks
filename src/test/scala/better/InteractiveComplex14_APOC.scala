package better

import io.gatling.core.Predef._
import io.gatling.core.scenario.Simulation
import io.gatling.http.Predef._

class InteractiveComplex14_APOC extends Simulation {
  // Setup server, username and password
  val httpProtocol = http
    .baseUrl("http://localhost:7474")
    .acceptHeader("application/json")
    .basicAuth("neo4j", "benchmark")

  val query =
    """MATCH path = allShortestPaths((person1:Person {id:$person1Id})-[:KNOWS*..15]-(person2:Person {id:$person2Id}))
             WITH nodes(path) AS pathNodes
             UNWIND pathNodes as node
             WITH collect(DISTINCT pathNodes) as paths, collect(distinct node) as nodes
             UNWIND nodes as node
             OPTIONAL MATCH (node)<-[:HAS_CREATOR]-(comment:Comment)-[:REPLY_OF]-()-[:HAS_CREATOR]->(poster) WHERE poster IN nodes
             WITH paths, node, collect(DISTINCT comment) as comments
             WITH paths, collect([node.id, node {posts:[(node)<-[:HAS_CREATOR]-(p:Post) | p], comments}]) as nodePostsAndComments
             WITH paths, apoc.map.fromPairs(nodePostsAndComments) as commentsMap
             UNWIND paths as pathNodes
             UNWIND apoc.coll.pairsMin(pathNodes) as pair
             WITH pathNodes, pair[0] as first, pair[1] as second, commentsMap
             WITH pathNodes, first, second, commentsMap[toString(first.id)] as firstMap, commentsMap[toString(second.id)] as secondMap
             WITH pathNodes, first, second, firstMap, secondMap, apoc.coll.sum([0] + [comment in firstMap.comments | apoc.coll.sum([0] + [(comment)-[:REPLY_OF]->(thing) WHERE thing in (secondMap.posts + secondMap.comments) | CASE WHEN thing:Post THEN 1.0 ELSE 0.5 END])]) as weight
             WITH pathNodes, weight + apoc.coll.sum([0] + [comment in secondMap.comments | apoc.coll.sum([0] + [(comment)-[:REPLY_OF]->(thing) WHERE thing in (firstMap.posts + firstMap.comments) | CASE WHEN thing:Post THEN 1.0 ELSE 0.5 END])]) as weight
             WITH pathNodes, sum(weight) as pathWeight
             RETURN [node in pathNodes | node.id] as personIdsInPath, pathWeight
             ORDER BY pathWeight DESC
    """.stripMargin.replaceAll("\n", " ")

  val statements = """{"statements" : [{"statement" : "%s", "parameters" : { "person1Id": 8796093030404, "person2Id": 26388279074461} }] }"""
    .format(query)

  val scn = scenario("better.InteractiveComplex14_APOC")
    .during(30 ) {
      exec(
        http("IC-14 (better)")
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