---
layout: default
title: Specification
permalink: /docs/specification/
---

## Overview
The proctor specification is JSON file describing the interface between a test-definition and application-specific code values. Each specification enumerates the tests it expects the test-matrix to contain and how each test's bucket maps to application-code. This file should be checked-in alongside the application's source code. This file also needs to be available at runtime so your application can ensure the test-matrix contains the required tests and buckets.

The specification's filename, path and contents are used by the [code generator][Codegen] and should be meaningful for your application.

```bash
# Example application
app: ToyStore
specification: ToyStoreGroups.json

# File Structure if using proctor-maven-plugin
.
├── src
|   ├── main
|       ├── proctor
|           ├── org/your/organisation/app/store/ToyStoreGroups.json

# File Structure if using proctor-ant-task
.
├── src
|   ├── resources
|       ├── org/your/organisation/app/store/ToyStoreGroups.json
```

or for split specifications


```bash

# File Structure if using proctor-maven-plugin
.
├── src
|   ├── main
|       ├── proctor
|           ├── org/your/organisation/app/store/ToyStore
|               ├── firsttest.json
|               ├── secondtest.json
|               ├── thirdtest.json
|               ├── providedcontext.json

# File Structure if using proctor-ant-task
.
├── src
|   ├── resources
|       ├── org/your/organisation/app/store
|           ├── firsttest.json
|           ├── secondtest.json
|           ├── thirdtest.json
|           ├── providedcontext.json
```

  Where `providedcontext.json` is a required file and the other _part_ specifications contain single test specifications. The names of the test are determined from the form `testname`.json. 
###Ant
  In ant, the generated class will still use the provided build.xml classnames and package names, however the input parameter must be the containing directory, and the output for the generated specifications must be specified.
###Maven
  In maven the generated class will use the standard of `Directory`Groups and `Directory`GroupsManager in naming files where `Directory` is the containing directory of the jsons (In this case: **ToyStore**) 
  

See [code generator][Codegen] for how to use the maven and ant plugins to generate code based on your application's specification.

## JSON Schema
| JSON Property | Description |
| ------------- | ----------- |
| `tests` | Collection of tests for this application. Maps from `$testName` to test description |
| `tests.$testName.buckets` | Collection of buckets for `$testName`. Maps `$bucketName => $bucketValue` |
| `tests.$testName.buckets.$bucketName` | integer value specifying the value for `$bucketName` in the `$testName`
| `tests.$testName.fallbackValue` | bucket value to be used if test-matrix cannot be loaded |
| `tests.$testName.payload.type` | (optional) string value indicating the type of payload for this test. See [Payloads][Payload] section below. |
| `tests.$testName.payload.validator` | (optional) string value indicating the [payload validator][PayloadValidator] for `$testName` |
| `tests.providedContext` | (optional) Collection of the variables to use in definition rules. Maps `$variableName => $variableType`. See [Context][Context] section below |

With a split specification each file defines a test that would be a property of tests, as well as provided context being separately defined.

## Defining Tests
{% gist parker/3bb0e94b9b238b48429f 0-ExampleGroups.json %}
The above specification defines a single test `bgcolortst` with five buckets: `inactive, altcolor1, altcolor2, altcolor3, altcolor4`.


## Context
A specification's `providedContext` defines the collection of application-specific variables available in the test definition rules. The `providedContext` maps variable names to Java types and is used to auto-generate methods with these types. In a web application, these variables typically map directly to the properties of a request. e.g. `logged-in`, `country`, `language` and `user-agent`. When using non-primitive types, like the custom `UserAgent` interface below, use the fully-qualified name.

If an application had the following four variables 

| Variable Name | Java Type |
| ------------- | --------- |
| country | String |
| loggedIn | boolean |
| language | String |
| us | com.indeed.example.UserAgent |

{% gist parker/3bb0e94b9b238b48429f 2-ExampleGroups-context.json %}

{% gist parker/3bb0e94b9b238b48429f 2-UserAgent.java %}

The test definition's `eligibility rules` and `allocation rules` can then reference these variables. The excerpt ([complete example](https://gist.github.com/parker/3bb0e94b9b238b48429f#file-2-definition-json)) below illustrates how a test-definition can reference the `country` and `ua` (UserAgent) variables to build targeted tests. See the [Test Definition]({{ site.baseurl }}/test-definition) section for a complete guide to the rule syntax.

```javascript
{
  "testType" : "USER",
  "constants" : {
    "COUNTRIES" : ["US", "CA"]
  },
  "rule" : "${proctor:contains(COUNTRIES, country)}",
  ...
  "allocations" : [ {
    "rule" : "${ua.android && ua.version > 4}",
    "ranges" : [ {
      "length" : 0.5,
      "bucketValue" : 0
    }, {
      "length" : 0.5,
      "bucketValue" : 1
    } ]
  }
  ...
}
```

## Payloads
Arbitrary data can be associated with each test bucket and delivered to your applications via the test-matrix. An application's specification can indicate if it expects a given test to have payloads by specifying the `payload.type` 

{% gist parker/3bb0e94b9b238b48429f 1-ExampleGroups-payload.json %}

Proctor supports 6 types of payloads: 

| Type | Java Type |
| ---- | ----------- |
| `stringValue` | `String` |
| `stringArray` | `String[]`|
| `doubleValue` | `double` |
| `doubleArray` | `double[]` |
| `longValue` | `long` |
| `longArray` | `long[]` |

The values for each bucket's payload are specified in the test-defintion (view [complete test defintion](https://gist.github.com/parker/3bb0e94b9b238b48429f#file-1-definition-json))

```javascript
{
  ...
  "buckets" : [ {
    "name" : "inactive",
    "value" : -1,
    "description" : "Inactive",
    "payload": {
      "stringValue": "#000000"
    }
  }, {
    "name" : "altcolor1",
    "value" : 0,
    "description" : "Background color 1",
    "payload": {
      "stringValue": "#000000"
    }
  ...
  ]
  ...
}
```

### payload validator (optional) 
An application can optionally define a `payload.validator` string in its specification. Similar to eligibility rules, this string is a boolean expression that should return `true` if a payload value is valid. During the test-matrix load-and-validate phase, each bucket's payload will be checked for compatibility using this expression. If no validator is provided, all payload values (of the correct type) will be considered valid.

```javascript
"payload": {
  "type": "stringValue",
  "validator": "${fn:startsWith(value, '#') && fn:length(value) == 7}"
}
```

The validator from [the above example](https://gist.github.com/parker/3bb0e94b9b238b48429f#file-1-exampleGroups-payload.json) enforces that each payload, referenced by `value` in expression, starts with "#" and is 7 characters long. e.g. '#000000'. Unlike *eligibility rules* and *allocation rules* the only available variable is `value`, the payload value. The specification context variables and test-constants are **NOT** available in the validator expression.

An application should always provide a default payload value in code and be resilient situations in which the test-matrix cannot be loaded.

## Multiple Tests
Multiple tests can be enumerated in an application's test specification by adding another entry to the `tests` map.

```javascript
{
    "tests" : {
        // Using a proctor test as a feature flag 
        "featureA": { "buckets" : {"inactive": -1, "disabled":0, "enabled":1}, "fallbackValue" : -1 },
        // horizontal/vertical/reverse layout test
        "layouttst": { "buckets" : {"inactive": -1, "horizontal":0, "vertical":1, "reverse":2}, "fallbackValue" : -1 }
    },
    "providedContext": {
        "lang": "String",
        "country" : "String"
    }
}
```

## Split Specifications
A split specification (as opposed to a single large specification) can be used as documented above and in [the Code Generation page]({{ site.baseurl }}/codegen). How to format these split specifications with a test similar to the **Multiple Tests** example would look like this:


`featureA.json`

```javascript
{
    // Using a proctor test as a feature flag 
     "buckets" : {"inactive": -1, "disabled":0, "enabled":1}, 
     "fallbackValue" : -1 
}
```

`layouttst.json`

```javascript
{
    // horizontal/vertical/reverse layout test
    "buckets" : {"inactive": -1, "horizontal":0, "vertical":1, "reverse":2}, 
    "fallbackValue" : -1 
}
```

`providedcontext.json`

```javascript
{
    "lang": "String",
    "country" : "String"
}
```



## A note on fallback values
A test's `fallbackValue` corresponds to the bucket value that should be used if a test matrix is not valid with the application's specification. The `proctor-loader` section describes how a test-definition can be invalid. In this scenario, the a new definition will be used in place of the invalid matrix. This definition	will have type=RANDOM and will allocation 100% of users to the test bucket signified by `fallbackValue`. Note, if `Identifiers.randomEnabled` is set to `false` the invalid test will not be allocated. See `using groups interface` for how to properly handle this in your application's code.

[Context]: #toc_3
[Payload]: #toc_4
[PayloadValidator]: #toc_5
[FallbackValues]: #toc_7
[CodeGen]: {{ site.baseurl }}/docs/codegen/