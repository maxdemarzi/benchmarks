package theirs

import io.gatling.core.Predef._
import io.gatling.core.scenario.Simulation
import io.gatling.http.Predef._

class InteractiveComplex8 extends Simulation {
  // Setup server, username and password
  val httpProtocol = http
    .baseUrl("http://localhost:7474")
    .acceptHeader("application/json")
    .basicAuth("neo4j", "benchmark")

  val query =
    """MATCH
        (start:Person {id:$personId})<-[:HAS_CREATOR]-(:Message)<-[:REPLY_OF]-(comment:Comment)-[:HAS_CREATOR]->(person:Person)
      RETURN
        person.id AS personId,
        person.firstName AS personFirstName,
        person.lastName AS personLastName,
        comment.creationDate AS commentCreationDate,
        comment.id AS commentId,
        comment.content AS commentContent
      ORDER BY commentCreationDate DESC, toInteger(commentId) ASC
      LIMIT 20
    """.stripMargin.replaceAll("\n", " ")

  val statements = """{"statements" : [{"statement" : "%s", "parameters" : { "personId": 24189255818757} }] }"""
    .format(query)

  val scn = scenario("theirs.InteractiveComplex8")
    .during(30 ) {
        exec(
          http("IC-8 (theirs)")
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