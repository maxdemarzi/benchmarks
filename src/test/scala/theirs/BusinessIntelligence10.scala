package theirs

import io.gatling.core.Predef._
import io.gatling.core.scenario.Simulation
import io.gatling.http.Predef._

class BusinessIntelligence10 extends Simulation {
  // Setup server, username and password
  val httpProtocol = http
    .baseUrl("http://localhost:7474")
    .acceptHeader("application/json")
    .basicAuth("neo4j", "benchmark")

  val query =
    """MATCH (tag:Tag {name: $tag})
      // score
      OPTIONAL MATCH (tag)<-[interest:HAS_INTEREST]-(person:Person)
      WITH tag, collect(person) AS interestedPersons
      OPTIONAL MATCH (tag)<-[:HAS_TAG]-(message:Message)-[:HAS_CREATOR]->(person:Person)
               WHERE message.creationDate > $date
      WITH tag, interestedPersons + collect(person) AS persons
      UNWIND persons AS person
      // poor man's disjunct union (should be changed to UNION + post-union processing in the future)
      WITH DISTINCT tag, person
      WITH
        tag,
        person,
        100 * length([(tag)<-[interest:HAS_INTEREST]-(person)| interest])
          + length([(tag)<-[:HAS_TAG]-(message:Message)-[:HAS_CREATOR]->(person) WHERE message.creationDate > $date | message])
        AS score
      OPTIONAL MATCH (person)-[:KNOWS]-(friend)
      WITH
        person,
        score,
        100 * length([(tag)<-[interest:HAS_INTEREST]-(friend)| interest])
          + length([(tag)<-[:HAS_TAG]-(message:Message)-[:HAS_CREATOR]->(friend) WHERE message.creationDate > $date | message])
        AS friendScore
      RETURN
        person.id,
        score,
        sum(friendScore) AS friendsScore
      ORDER BY
        score + friendsScore DESC,
        person.id ASC
      LIMIT 100
    """.stripMargin.replaceAll("\n", " ")

  val statements = """{"statements" : [{"statement" : "%s", "parameters" : { "tag": "John_Rhys-Davies", "date": 20120122000000000 } }] }"""
    .format(query)

  val scn = scenario("theirs.BusinessIntelligence10")
    .during(30 ) {
        exec(
          http("BI-10 (theirs)")
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