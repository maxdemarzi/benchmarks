package theirs

import io.gatling.core.Predef._
import io.gatling.core.scenario.Simulation
import io.gatling.http.Predef._

class BusinessIntelligence1 extends Simulation {
  // Setup server, username and password
  val httpProtocol = http
    .baseUrl("http://localhost:7474")
    .acceptHeader("application/json")
    .basicAuth("neo4j", "benchmark")

  val query =
    """MATCH (message:Message)
      WHERE message.creationDate < $date
      WITH count(message) AS totalMessageCountInt
      WITH toFloat(totalMessageCountInt) AS totalMessageCount
      MATCH (message:Message)
      WHERE message.creationDate < $date
        AND message.content IS NOT NULL
      WITH
        totalMessageCount,
        message,
        message.creationDate/10000000000000 AS year
      WITH
        totalMessageCount,
        year,
        message:Comment AS isComment,
        CASE
          WHEN message.length <  40 THEN 0
          WHEN message.length <  80 THEN 1
          WHEN message.length < 160 THEN 2
          ELSE                           3
        END AS lengthCategory,
        count(message) AS messageCount,
        floor(avg(message.length)) AS averageMessageLength,
        sum(message.length) AS sumMessageLength
      RETURN
        year,
        isComment,
        lengthCategory,
        messageCount,
        averageMessageLength,
        sumMessageLength,
        messageCount / totalMessageCount AS percentageOfMessages
      ORDER BY
        year DESC,
        isComment ASC,
        lengthCategory ASC
    """.stripMargin.replaceAll("\n", " ")

  val statements = """{"statements" : [{"statement" : "%s", "parameters" : { "date": 20120501000000000 } }] }"""
    .format(query)

  val scn = scenario("theirs.BusinessIntelligence1")
    .during(30 ) {
        exec(
          http("BI-1 (theirs)")
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