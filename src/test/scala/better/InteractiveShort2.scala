package better

import io.gatling.core.Predef._
import io.gatling.core.scenario.Simulation
import io.gatling.http.Predef._

class InteractiveShort2 extends Simulation {
  // Setup server, username and password
  val httpProtocol = http
    .baseUrl("http://localhost:7474")
    .acceptHeader("application/json")
    .basicAuth("neo4j", "benchmark")

  val query =
    """MATCH (:Person {id:$personId})<-[:HAS_CREATOR]-(message)
      WITH
       message,
       message.id AS messageId,
       message.creationDate AS messageCreationDate
      ORDER BY messageCreationDate DESC, messageId ASC
      LIMIT 10
      MATCH (message)-[:REPLY_OF*0..]->(post:Post),
            (post)-[:HAS_CREATOR]->(person)
      RETURN
       messageId,
       messageCreationDate,
       coalesce(message.imageFile,message.content) AS messageContent,
       post.id AS postId,
       person.id AS personId,
       person.firstName AS personFirstName,
       person.lastName AS personLastName
      ORDER BY messageCreationDate DESC, messageId ASC
    """.stripMargin.replaceAll("\n", " ")

  val statements = """{"statements" : [{"statement" : "%s", "parameters" : { "personId": 17592186053137 } }] }"""
    .format(query)

  val scn = scenario("better.InteractiveShort2")
    .during(30 ) {
        exec(
          http("IS-2 (better)")
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