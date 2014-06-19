# Proctor Service

Proctor Service is a Java web application that provides a simple REST API to [Proctor](https://github.com/indeedeng/proctor).

Through this API, other programming languages only need an HTTP client to use Proctor. This makes it easy to get test groups from a non-JVM web backend language like PHP or Python.

## Getting Started

1. ```git clone``` the repository or download the sources.

2. Copy the three configuration files in proctor-service-deploy/src/main/webapp/WEB-INF/example/ to any directory.

    These contain some basic Proctor tests that let you quickly try out Proctor Service.

3. Edit the paths in proctor-service-deploy/src/main/webapp/WEB-INF/config/service-base.properties to point to that directory.

    ```ini
    proctor.test.matrix.path=/your/path/to/proctor-tests-matrix.json
    proctor.test.specification.path=/your/path/to/proctor-specification.json
    proctor.service.config.path=/your/path/to/service-config.json
    proctor.service.reload.seconds=10
    ```

4. Run ```mvn package``` to create a .war package.

5. Run ```java -jar proctor-service-deploy/target/dependency/webapp-runner.jar proctor-service-deploy/target/*.war```. This starts a local web server.

6. Visit [http://localhost:8080/groups/identify?ctx.country=US&ctx.loggedIn=true&id.USER=pa5xq0lz4n80](http://localhost:8080/groups/identify?ctx.country=US&ctx.loggedIn=true&id.USER=pa5xq0lz4n80) in your browser.

    Like determineTestGroups(), this returns group assignments given context variables and identifiers.

    If you refresh, group assignments stay the same. This ensures your users don't change test groups as they browse.

    Try adding random characters to id.USER (which typically represents a tracking cookie). The assigned test groups will change.

    _buttoncolortst_ has a string payload attached to it. You can use payloads to try different colors or text by changing the test matrix. No redeploy of your web application is needed!

    The user agent is parsed and can be used in more complex Proctor rules. For example, the _mobileonly_ test only appears if the user agent is a mobile device.

    You can configure the API to accept context variables and identifiers from any source in _service-config.json_. The example is configured to look at the HTTP header _User-Agent_, but this can come from any header name or query parameter.
