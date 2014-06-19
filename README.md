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

## Configuration

There are four configuration files required by Proctor Service.

### service.properties

This is a simple ini-like file that defines paths to other configuration files and sets some other simple properties.

Proctor Service searches for this properties file in the following places in this order:

1. WEB-INF/config/service-base.properties (This is included in the repository. The included default sets the path of the configuration files to the ${catalina.base}/conf/proctor/ directory.)

2. ${catalina.base}/conf/service.properties

3. Path pointed to by the _propertyPlaceholderResourceLocation_ Tomcat context parameter.

The properties file requires the following fields:

* proctor.test.matrix.path

    Path to the Proctor test matrix.

* proctor.test.specification.path

    Path to the Proctor test specification.

* proctor.service.config.path

    Path to the service configuration.

* proctor.service.reload.seconds

    An integer defining the number of seconds between reloads of the Proctor test matrix.

    Decreasing this number will increase the frequency of checking whether the test matrix has changed.

### Test Matrix

A JSON file with a list of all test definitions used by Proctor Service, including all test buckets and payload values.

See: [Proctor Test Matrix Schema](http://indeedeng.github.io/proctor/docs/matrix-schema/)

### Test Specification

A simple JSON file that describes which tests Proctor Service is using. This is mostly a repeat of tests described in the test matrix but with a slightly different format.

The _providedContext_ section is not used in Proctor Service and can safely be omitted. Types are instead defined in the service configuration file.

This configuration file will hopefully be removed from Proctor Service soon. It is only required due to Proctor internals.

See: [Proctor Specification](http://indeedeng.github.io/proctor/docs/specification/)

### Service Configuration

A JSON file that describes the variables that users pass into the /groups/identify API call, including whether they are passed in as a query parameter or as a header.

Here is a full example of a service configuration:

```json
{
    "context": {
        "country": {
            "source": "QUERY",
            "type": "String"
        },
        "loggedIn": {
            "source": "QUERY",
            "type": "boolean",
            "defaultValue": false
        },
        "userAgent": {
            "source": "HEADER",
            "sourceKey": "User-Agent",
            "type": "UserAgent"
        }
    },
    "identifiers": {
        "USER": {
            "source": "QUERY"
        },
        "ACCOUNT": {
            "source": "QUERY",
            "sourceKey": "acctid"
        }
    }
}
```

_context_ contains a map of all context variables, and _identifiers_ contains a map of all identifiers.

For context variables, the key is the name of context variable, which is also used for Proctor rules in the test matrix.

For identifiers, the key is the [test type](http://indeedeng.github.io/proctor/docs/terminology/#toc_11), such as _USER_ or _ACCOUNT_.

#### Configuration Values

**All variables** support the following configuration values:

* source _(required)_

    Where Proctor Service looks for this variable in the /groups/identify web request.

    Acceptable values: QUERY or HEADER

* sourceKey _(optional)_

    At which key Proctor Service looks for this variable in the source.

    For QUERY, this is the name of the query parameter. For HEADER, this is the header name.

    If the sourceKey is not specified, the source key is defaulted to this configuration's key. For example, _country_'s default source key is _country_.

**Context variables** support additional configuration values:

* type _(required)_

    Name of the type this context variable will be converted to for evaluation in Proctor rules.

    Acceptable values include any of the [eight Java primitive types](http://docs.oracle.com/javase/tutorial/java/nutsandbolts/datatypes.html), their object wrapper classes, _String_, and _UserAgent_.

* defaultValue _(optional)_

    If this context variable is not included in the request, this default value will be used for that variable.

    If a defaultValue is not specified, then this context variable is required and must be included in every /groups/identify API call.

    The JSON type of this value does not matter.
