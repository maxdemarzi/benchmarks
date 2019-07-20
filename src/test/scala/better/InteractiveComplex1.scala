package better

import io.gatling.core.Predef._
import io.gatling.core.scenario.Simulation
import io.gatling.http.Predef._

class InteractiveComplex1 extends Simulation {
  // Setup server, username and password
  val httpProtocol = http
    .baseUrl("http://localhost:7474")
    .acceptHeader("application/json")
    .basicAuth("neo4j", "benchmark")

  val query =
    """MATCH (p:Person {id:$personId}), (friend:Person {firstName:$firstName})
       WITH p, friend
       MATCH path = shortestPath((p)-[:KNOWS*1..3]-(friend))
       WITH min(length(path)) AS distance, friend
       ORDER BY distance ASC, friend.lastName ASC, toInteger(friend.id) ASC
       LIMIT 20
       MATCH (friend)-[:IS_LOCATED_IN]->(friendCity)
       OPTIONAL MATCH (friend)-[studyAt:STUDY_AT]->(uni)-[:IS_LOCATED_IN]->(uniCity)
       WITH
         friend,
         collect(
           CASE uni.name
             WHEN null THEN null
             ELSE [uni.name, studyAt.classYear, uniCity.name]
           END
         ) AS unis,
         friendCity,
         distance
       OPTIONAL MATCH (friend)-[workAt:WORK_AT]->(company)-[:IS_LOCATED_IN]->(companyCountry)
       WITH
         friend,
         collect(
           CASE company.name
             WHEN null THEN null
             ELSE [company.name, workAt.workFrom, companyCountry.name]
           END
         ) AS companies,
         unis,
         friendCity,
         distance
       RETURN
         friend.id AS friendId,
         friend.lastName AS friendLastName,
         distance AS distanceFromPerson,
         friend.birthday AS friendBirthday,
         friend.creationDate AS friendCreationDate,
         friend.gender AS friendGender,
         friend.browserUsed AS friendBrowserUsed,
         friend.locationIP AS friendLocationIp,
         friend.email AS friendEmails,
         friend.speaks AS friendLanguages,
         friendCity.name AS friendCityName,
         unis AS friendUniversities,
         companies AS friendCompanies
       ORDER BY distanceFromPerson ASC, friendLastName ASC, friendId ASC
       LIMIT 20
    """.stripMargin.replaceAll("\n", " ")

  val statements = """{"statements" : [{"statement" : "%s", "parameters" : { "personId": 30786325583618, "firstName": "Carmen" } }] }"""
    .format(query)

  val scn = scenario("better.InteractiveComplex1")
    .during(30 ) {
        exec(
          http("IC-1 (better)")
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