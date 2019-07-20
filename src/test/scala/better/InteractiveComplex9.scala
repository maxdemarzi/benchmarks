package better

import io.gatling.core.Predef._
import io.gatling.core.scenario.Simulation
import io.gatling.http.Predef._

class InteractiveComplex9 extends Simulation {
  // Setup server, username and password
  val httpProtocol = http
    .baseUrl("http://localhost:7474")
    .acceptHeader("application/json")
    .basicAuth("neo4j", "benchmark")

  val query =
    """MATCH (person:Person {id:$personId})-[:KNOWS*1..2]-(friend)
       WHERE NOT person=friend
       WITH DISTINCT friend
       MATCH (friend)<-[:HAS_CREATOR]-(message)
       WHERE message.creationDate < $maxDate
       WITH friend, message
       ORDER BY message.creationDate DESC, message.id ASC
       LIMIT 20
       RETURN message.id AS messageId,
              coalesce(message.content,message.imageFile) AS messageContent,
              message.creationDate AS messageCreationDate,
              friend.id AS personId,
              friend.firstName AS personFirstName,
              friend.lastName AS personLastName
    """.stripMargin.replaceAll("\n", " ")

  val statements = """{"statements" : [{"statement" : "%s", "parameters" : { "personId": 13194139542834, "maxDate": 20120524003033020} }] }"""
    .format(query)

  val scn = scenario("better.InteractiveComplex9")
    .during(30 ) {
        exec(
          http("IC-9 (better)")
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