package theirs

import io.gatling.core.Predef._
import io.gatling.core.scenario.Simulation
import io.gatling.http.Predef._

class InteractiveComplex7 extends Simulation {
  // Setup server, username and password
  val httpProtocol = http
    .baseUrl("http://localhost:7474")
    .acceptHeader("application/json")
    .basicAuth("neo4j", "benchmark")

  val query =
    """MATCH (person:Person {id:$personId})<-[:HAS_CREATOR]-(message:Message)<-[like:LIKES]-(liker:Person)
       WITH liker, message, like.creationDate AS likeTime, person
       ORDER BY likeTime DESC, toInteger(message.id) ASC
       WITH
         liker,
         head(collect({msg: message, likeTime: likeTime})) AS latestLike,
         person
       RETURN
         liker.id AS personId,
         liker.firstName AS personFirstName,
         liker.lastName AS personLastName,
         latestLike.likeTime AS likeCreationDate,
         latestLike.msg.id AS commentOrPostId,
         CASE exists(latestLike.msg.content)
           WHEN true THEN latestLike.msg.content
           ELSE latestLike.msg.imageFile
         END AS commentOrPostContent,
         latestLike.msg.creationDate AS commentOrPostCreationDate,
         not((liker)-[:KNOWS]-(person)) AS isNew
       ORDER BY likeCreationDate DESC, toInteger(personId) ASC
       LIMIT 20
    """.stripMargin.replaceAll("\n", " ")

  val statements = """{"statements" : [{"statement" : "%s", "parameters" : { "personId": 17592186053137} }] }"""
    .format(query)

  val scn = scenario("theirs.InteractiveComplex7")
    .during(30 ) {
        exec(
          http("IC-7 (theirs)")
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