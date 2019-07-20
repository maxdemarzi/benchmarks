package theirs

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
    """MATCH (:Person {id:$personId})-[:KNOWS*1..2]-(friend:Person)<-[:HAS_CREATOR]-(message:Message)
      WHERE message.creationDate < $maxDate
      RETURN DISTINCT
        friend.id AS personId,
        friend.firstName AS personFirstName,
        friend.lastName AS personLastName,
        message.id AS commentOrPostId,
        CASE exists(message.content)
          WHEN true THEN message.content
          ELSE message.imageFile
        END AS commentOrPostContent,
        message.creationDate AS commentOrPostCreationDate
      ORDER BY message.creationDate DESC, toInteger(message.id) ASC
      LIMIT 20
    """.stripMargin.replaceAll("\n", " ")

  val statements = """{"statements" : [{"statement" : "%s", "parameters" : { "personId": 13194139542834, "maxDate": 20120524003033020} }] }"""
    .format(query)

  val scn = scenario("theirs.InteractiveComplex9")
    .during(30 ) {
        exec(
          http("IC-9 (theirs)")
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