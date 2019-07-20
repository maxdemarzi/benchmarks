package theirs

import io.gatling.core.Predef._
import io.gatling.core.scenario.Simulation
import io.gatling.http.Predef._

class BusinessIntelligence2 extends Simulation {
  // Setup server, username and password
  val httpProtocol = http
    .baseUrl("http://localhost:7474")
    .acceptHeader("application/json")
    .basicAuth("neo4j", "benchmark")

  val query =
    """MATCH
        (country:Country)<-[:IS_PART_OF]-(:City)<-[:IS_LOCATED_IN]-(person:Person)
        <-[:HAS_CREATOR]-(message:Message)-[:HAS_TAG]->(tag:Tag)
      WHERE message.creationDate >= $startDate
        AND message.creationDate <= $endDate
        AND (country.name = $country1 OR country.name = $country2)
      WITH
        country.name AS countryName,
        message.creationDate/100000000000%100 AS month,
        person.gender AS gender,
        floor((20130101 - person.birthday) / 10000 / 5.0) AS ageGroup,
        tag.name AS tagName,
        message
      WITH
        countryName, month, gender, ageGroup, tagName, count(message) AS messageCount
      WHERE messageCount > 100
      RETURN
        countryName,
        month,
        gender,
        ageGroup,
        tagName,
        messageCount
      ORDER BY
        messageCount DESC,
        tagName ASC,
        ageGroup ASC,
        gender ASC,
        month ASC,
        countryName ASC
      LIMIT 100
    """.stripMargin.replaceAll("\n", " ")

  val statements = """{"statements" : [{"statement" : "%s", "parameters" : { "startDate": 20080318008053020, "endDate":  20120524003033020, "country1": "Spain", "country2": "Germany" } }] }"""
    .format(query)

  val scn = scenario("theirs.BusinessIntelligence2")
    .during(30 ) {
        exec(
          http("BI-2 (theirs)")
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