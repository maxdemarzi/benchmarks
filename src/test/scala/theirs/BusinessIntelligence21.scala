package theirs

import io.gatling.core.Predef._
import io.gatling.core.scenario.Simulation
import io.gatling.http.Predef._

class BusinessIntelligence21 extends Simulation {
  // Setup server, username and password
  val httpProtocol = http
    .baseUrl("http://localhost:7474")
    .acceptHeader("application/json")
    .basicAuth("neo4j", "benchmark")

  val query =
    """MATCH (country:Country {name: $country})
      WITH
        country,
        $endDate/10000000000000   AS endDateYear,
        $endDate/100000000000%100 AS endDateMonth
      MATCH
        (country)<-[:IS_PART_OF]-(:City)<-[:IS_LOCATED_IN]-(zombie:Person)
      OPTIONAL MATCH
        (zombie)<-[:HAS_CREATOR]-(message:Message)
      WHERE zombie.creationDate  < $endDate
        AND message.creationDate < $endDate
      WITH
        country,
        zombie,
        endDateYear,
        endDateMonth,
        zombie.creationDate/10000000000000   AS zombieCreationYear,
        zombie.creationDate/100000000000%100 AS zombieCreationMonth,
        count(message) AS messageCount
      WITH
        country,
        zombie,
        12 * (endDateYear  - zombieCreationYear )
           + (endDateMonth - zombieCreationMonth)
           + 1 AS months,
        messageCount
      WHERE messageCount / months < 1
      WITH
        country,
        collect(zombie) AS zombies
      UNWIND zombies AS zombie
      OPTIONAL MATCH
        (zombie)<-[:HAS_CREATOR]-(message:Message)<-[:LIKES]-(likerZombie:Person)
      WHERE likerZombie IN zombies
      WITH
        zombie,
        count(likerZombie) AS zombieLikeCount
      OPTIONAL MATCH
        (zombie)<-[:HAS_CREATOR]-(message:Message)<-[:LIKES]-(likerPerson:Person)
      WHERE likerPerson.creationDate < $endDate
      WITH
        zombie,
        zombieLikeCount,
        count(likerPerson) AS totalLikeCount
      RETURN
        zombie.id,
        zombieLikeCount,
        totalLikeCount,
        CASE totalLikeCount
          WHEN 0 THEN 0.0
          ELSE zombieLikeCount / toFloat(totalLikeCount)
        END AS zombieScore
      ORDER BY
        zombieScore DESC,
        zombie.id ASC
      LIMIT 100
    """.stripMargin.replaceAll("\n", " ")

  val statements = """{"statements" : [{"statement" : "%s", "parameters" : { "country": "Ethiopia", "endDate": 20130101000000000 } }] }"""
    .format(query)

  val scn = scenario("theirs.BusinessIntelligence21")
    .during(30 ) {
        exec(
          http("BI-21 (theirs)")
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