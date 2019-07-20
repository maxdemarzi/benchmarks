package better

import io.gatling.core.Predef._
import io.gatling.core.scenario.Simulation
import io.gatling.http.Predef._

class InteractiveComplex2 extends Simulation {
  // Setup server, username and password
  val httpProtocol = http
    .baseUrl("http://localhost:7474")
    .acceptHeader("application/json")
    .basicAuth("neo4j", "benchmark")

  val query =
    """MATCH (:Person {id:$personId})-[:KNOWS]-(friend)<-[:HAS_CREATOR]-(message)
       WHERE message.creationDate <= $maxDate
       RETURN
         friend.id AS personId,
         friend.firstName AS personFirstName,
         friend.lastName AS personLastName,
         message.id AS messageId,
         COALESCE(message.content, message.imageFile) AS messageContent,
         message.creationDate AS messageCreationDate
       ORDER BY messageCreationDate DESC, messageId ASC
       LIMIT 20
    """.stripMargin.replaceAll("\n", " ")

  val statements = """{"statements" : [{"statement" : "%s", "parameters" : { "personId": 17592186052613, "maxDate": 20120501000000000 } }] }"""
    .format(query)

  val scn = scenario("better.InteractiveComplex2")
    .during(30 ) {
        exec(
          http("IC-2 (better)")
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