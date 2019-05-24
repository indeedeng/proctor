# proctor-maven-plugin

By including this plugin in a maven build, new maven tasks become available.

## Code generation tasks

These tasks are added to the `GENERATE_SOURCES` Maven lifecycle stage.

* `mvn generate` generates java code from test specifications (default from src/main/proctor)
* `mvn generate-test` generates java code from test specifications (default from src/test/proctor)
* `mvn generate-js` generates java code from test specifications (default from src/main/proctor)
* `mvn generate-test-js` generates java code from test specifications (default from src/test/proctor)

## Matrix builder task

`mvn generate-matrix` will convert a directory with proctor matrix files in standard layout to a file called `proctor-test-matrix.json`.
