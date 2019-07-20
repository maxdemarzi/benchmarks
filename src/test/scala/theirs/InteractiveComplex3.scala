package theirs

import io.gatling.core.Predef._
import io.gatling.core.scenario.Simulation
import io.gatling.http.Predef._

class InteractiveComplex3 extends Simulation {
  // Setup server, username and password
  val httpProtocol = http
    .baseUrl("http://localhost:7474")
    .acceptHeader("application/json")
    .basicAuth("neo4j", "benchmark")

  val query =
    """MATCH (person:Person {id:$personId})-[:KNOWS*1..2]-(friend:Person)<-[:HAS_CREATOR]-(messageX:Message),
      (messageX)-[:IS_LOCATED_IN]->(countryX:Place)
      WHERE
        not(person=friend)
        AND not((friend)-[:IS_LOCATED_IN]->()-[:IS_PART_OF]->(countryX))
        AND countryX.name=$countryXName AND messageX.creationDate>=$startDate
        AND messageX.creationDate < ($startDate + $duration)
      WITH friend, count(DISTINCT messageX) AS xCount
      MATCH (friend)<-[:HAS_CREATOR]-(messageY:Message)-[:IS_LOCATED_IN]->(countryY:Place)
      WHERE
        countryY.name=$countryYName
        AND not((friend)-[:IS_LOCATED_IN]->()-[:IS_PART_OF]->(countryY))
        AND messageY.creationDate>=$startDate
        AND messageY.creationDate<($startDate + $duration)
      WITH
        friend.id AS personId,
        friend.firstName AS personFirstName,
        friend.lastName AS personLastName,
        xCount,
        count(DISTINCT messageY) AS yCount
      RETURN
        personId,
        personFirstName,
        personLastName,
        xCount,
        yCount,
        xCount + yCount AS count
      ORDER BY count DESC, toInteger(personId) ASC
      LIMIT 20
    """.stripMargin.replaceAll("\n", " ")

  val statements = """{"statements" : [{"statement" : "%s", "parameters" : { "personId": 17592186055119, "startDate": 20120501000000000, "duration":42000000000, "countryXName":'Laos', "countryYName":'Scotland' } }] }"""
    .format(query)

  val scn = scenario("theirs.InteractiveComplex3")
    .during(30 ) {
        exec(
          http("IC-3 (theirs)")
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