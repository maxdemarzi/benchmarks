# Performance Tests

Instructions
------------ 

Open project in IntelliJ.

Right-Click on src/test/scala/Engine and choose "Run 'Engine' "

Choose Simulation to run.

Simulations in src/test/scala directory have hardcoded "benchmark" for password. 

Data
----


[Scale Factor-10 graph.db](https://www.dropbox.com/s/n9io553w70c756n/graph.db.zip?dl=0)

The Neo4j graph.db database linked above only has the minimal indexes.

InteractiveComplex queries require:

CREATE INDEX ON :Person(firstName);

