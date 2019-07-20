package theirs

import io.gatling.core.Predef._
import io.gatling.core.scenario.Simulation
import io.gatling.http.Predef._

class InteractiveComplex12 extends Simulation {
  // Setup server, username and password
  val httpProtocol = http
    .baseUrl("http://localhost:7474")
    .acceptHeader("application/json")
    .basicAuth("neo4j", "benchmark")

  val query =
    """MATCH (:Person {id:$personId})-[:KNOWS]-(friend:Person)<-[:HAS_CREATOR]-(comment:Comment)-[:REPLY_OF]->(:Post)-[:HAS_TAG]->(tag:Tag),
        (tag)-[:HAS_TYPE]->(tagClass:TagClass)-[:IS_SUBCLASS_OF*0..]->(baseTagClass:TagClass)
      WHERE tagClass.name = $tagClassName OR baseTagClass.name = $tagClassName
      RETURN
        friend.id AS personId,
        friend.firstName AS personFirstName,
        friend.lastName AS personLastName,
        collect(DISTINCT tag.name) AS tagNames,
        count(DISTINCT comment) AS replyCount
      ORDER BY replyCount DESC, toInteger(personId) ASC
      LIMIT 20
    """.stripMargin.replaceAll("\n", " ")

  val statements = """{"statements" : [{"statement" : "%s", "parameters" : { "personId": 17592186052613, "tagClassName": "BasketballPlayer"} }] }"""
    .format(query)

  val scn = scenario("theirs.InteractiveComplex12")
    .during(30 ) {
        exec(
          http("IC-12 (theirs)")
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