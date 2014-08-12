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

There are three configuration files required by Proctor Service.

### service.properties

This is a simple ini-like file that defines paths to other configuration files and sets some other simple properties.

Proctor Service searches for this properties file in the following places in this order:

1. WEB-INF/config/service-base.properties (This is included in the repository. The included default sets the path of the configuration files to the ${catalina.base}/conf/proctor/ directory.)

2. ${catalina.base}/conf/service.properties

3. Path pointed to by the _propertyPlaceholderResourceLocation_ Tomcat context parameter.

The properties file requires the following fields:

* proctor.test.matrix.path

    Path to the Proctor test matrix.

* proctor.service.config.path

    Path to the service configuration.

* proctor.service.reload.seconds

    An integer defining the number of seconds between reloads of the Proctor test matrix.

    Decreasing this number will increase the frequency of checking whether the test matrix has changed.

### Test Matrix

A JSON file with a list of all test definitions used by Proctor Service, including all test buckets and payload values.

See: [Proctor Test Matrix Schema](http://indeedeng.github.io/proctor/docs/matrix-schema/)

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

For context variables, the key is the name of the context variable, which is also used for Proctor rules in the test matrix.

For identifiers, the key is the [test type](http://indeedeng.github.io/proctor/docs/terminology/#test-type), such as _USER_ or _ACCOUNT_.

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

## API Endpoints

All API responses are wrapped in a JSON envelope with _data_ and _meta_ fields.

```json
{
    "data": {
        ...
    },
    "meta": {
        "status": 200
    }
}
```

_data_ contains the true content of the API response. _data_ is always present, but it can be empty.

_meta_ contains metadata about the API response. _meta.status_ contains the HTTP response code.

If there was an error with the API request, _meta.error_ will contain a string with an error message. For example:

```json
{
    "data": { },
    "meta": {
        "status": 400,
        "error": "Request must have at least one identifier."
    }
}
```

### GET /groups/identify

From given identifiers and context variables, determine the test groups that should be used.

#### Parameters

In addition to the query parameters below, the service configuration can declare that certain variables come from the request headers instead.

* ctx.{sourceKey} _(context variables without a defaultValue are required)_

    All query parameters starting with _ctx._ are treated as context variables and converted to the type specified in the service configuration.

    Context variables are used in evaluating Proctor [rule expressions](http://indeedeng.github.io/proctor/docs/test-rules/).

* id.{sourceKey} _(at least one identifier is required)_

    All query parameters starting with _id._ are treated as identifiers.

    Identifiers are used to differentiate different users based on tracking cookie, account id, email, or anything else that is supported by Proctor.

* test _(optional)_

    Filter the returned tests by name.

    A comma-separated list of test names.

    Use this parameter to return only the tests that your web application is interested in and can supply the relevant context variables for rules.

    If this parameter is not present, no filter is applied, and the API attempts to return **all tests in the test matrix**. If your test matrix is large and contains tests and rules from many different projects, this may be a bad idea. If a rule fails to execute because a context variable was missing, the API will log an error message to the logfile/console, skip the test, and continue.

    If this parameter is empty, zero tests are returned, so _groups_ will be empty. The audit information is still accurate.

* prforceGroups _(optional)_

    Force certain test group assignments in this API request. This lets privileged users (developers) test their groups.

    This parameter works exactly like Proctor's [prforceGroups](http://indeedeng.github.io/proctor/docs/using-groups/#forceGroups).

    Formatted as a comma-separated list of groups strings in the format of {groupName}{bucketValue}. For example: _prforceGroups=buttoncolortst2,newfeaturerollout0_

    This parameter is deliberately simple so that your web backend can easily implement force groups like Proctor:

    1. If the user is privileged and includes a _prforceGroups_ query parameter in their request, store its value in a session cookie and use the value in this API call.

    2. If the user is privileged and has that cookie and does not use the query parameter, use the cookie's value in this API call.

           If you use a simple cookie instead of signed cookies or sessions IDs, ensure that you check for user privilege before using the cookie value. Otherwise this is a **security issue** because ordinary users could manually set that cookie to force groups.

    3. Otherwise, do not include the _prforceGroups_ parameter (or use a blank string).

Here is an example of a request using all of these parameters:

```
GET /groups/identify
User-Agent: Mozilla/5.0 (X11; Linux x86_64; rv:28.0) Gecko/20100101 Firefox/28.0

ctx.country=US
ctx.loggedIn=true
id.USER=pa5xq0lz4n80
id.acctid=5083
test=buttoncolortst,newfeaturerollout,mobileonly
prforceGroups=buttoncolortst2,newfeaturerollout0
```

#### Response

```json
{
    "data": {
        "groups": {
            ...
        },
        "context": {
            "userAgent": {
                ...
            },
            "loggedIn": true,
            "country": "US"
        },
        "audit": {
            "version": 1,
            "updated": 1,
            "updatedBy": "example"
        },
    "meta": {
        "status": 200
    }
}
```

The response to the /groups/identify API call contains three major components:

##### groups

_groups_ contains a mapping of test name to values associated with the test group assignment.

```json
{
    "buttoncolortst": {
        "name": "control",
        "value": 0,
        "version": 1,
        "payload": {
            "stringValue": "#C0C0C0"
        }
    },
    "newfeaturerollout": {
        "name": "inactive",
        "value": -1,
        "version": 1
    }
}
```

The values include the bucket _name_, the bucket _value_, the _version_ of the test definition, and possibly a _payload_.

_payload_ is only included if the test definition contains payload values and a [payload type](http://indeedeng.github.io/proctor/docs/specification/#payloads).

If test groups you are expecting are not in the _groups_ mapping, it's possible that an [eligibility rule](http://indeedeng.github.io/proctor/docs/terminology/#eligibility-rule) excludes them, or there was no identifier with the proper test type. Ensure that you have appropriate default behavior for these situations. This default behavior would also be useful if your Proctor Service instance goes down or stops responding.

##### context

_context_ contains all the context variables with their converted values.

The user agent output is especially helpful to help debug any rules based on user agent.

##### audit

_audit_ contains the audit of the test matrix just like /proctor/matrix/audit.

This can be useful for caching.

### GET /proctor/matrix

Returns the entire test matrix.

### GET /proctor/matrix/audit

Returns the audit of the test matrix.

The response includes a _version_ number, an _updated_ timestamp, and an _updatedBy_ name.

### GET /proctor/matrix/definition/{testname}

Returns the definition for a specific test as defined in the test matrix.

### GET /config/context

Returns the configured context variables from the service configuration.

### GET /config/identifiers

Returns the identifiers from the service configuration.

## See Also

* [Proctor Documentation](http://indeedeng.github.io/proctor/)

* [Proctor Github Repo](https://github.com/indeedeng/proctor)

* [Proctor Webapp](https://github.com/indeedeng/proctor-webapp) for editing the test matrix.

* [indeedeng-proctor-users](https://groups.google.com/forum/#!forum/indeedeng-proctor-users) for questions and comments.

* [Proctor Blog Post](http://engineering.indeed.com/blog/2014/06/proctor-a-b-testing-framework/) on the Indeed Engineering blog.

* [Proctor Tech Talk](http://engineering.indeed.com/talks/managing-experiments-behavior-dynamically-proctor/)
