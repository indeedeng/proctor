---
layout: default
title: Code Generator
permalink: /docs/codegen/
---

Use the 'proctor-codegen' module to generate a Java representation of an application's test specification. The module generates a concrete implementation of `AbstractGroups` and `AbstractGroupsManager`.
You can also generate a JavaScript representation. You'll need to use your Java-generated code to compute group allocations, then pass them to the client side to use with your generated JavaScript.

**AbstractGroups** -
Provides an interface for consuming and accessing the groups for a user. Accessor methods are generated for each test and test-bucket.

**AbstractGroupsManager** -
Provides an interface for determining the groups for a user based on the provided `Identifiers` and `context`.

### Example specification

Consider the following [ExampleGroups.json](https://gist.github.com/parker/3bb0e94b9b238b48429f#file-2-examplegroups-context-json) specification

<pre><code>{
    "tests" : {
        "bgcolortst": {
            "buckets": {
                "inactive":-1,
                "altcolor1":0,
                "altcolor2":1,
                "altcolor3":2,
                "altcolor4":3
            },
            "fallbackValue": -1
        }
    },
    "providedContext": {
        "country": "String",
        "loggedIn": "boolean",
        "language": "String",
        "ua": "com.indeed.example.UserAgent"
    }
}</code></pre>


### Generated ExampleGroupsManager.java

```java
package com.indeed.example;

public class ExampleGroupsManager extends AbstractGroupsManager {
  public ExampleGroupsManager(final Supplier<Proctor> proctorSource) {
      super(proctorSource);
  }

  public ProctorResult determineBuckets(final Identifiers identifiers,
                                        final String country,
                                        final boolean loggedIn,
                                        final String language,
                                        final com.indeed.example.UserAgent ua);

  public ProctorResult determineBuckets(final HttpServletRequest request,
                                        final HttpServletResponse response,
                                        final Identifiers identifiers,
                                        final boolean allowForcedGroups,
                                        final String country,
                                        final boolean loggedIn,
                                        final String language,
                                        final com.indeed.example.UserAgent ua);
}
```

Key points:

- The `Supplier<Proctor> proctorSource` constructor argument should provide a loaded Proctor instance. Typically, this is an implementation of `AbstractProctorLoader`. See [proctor-loader][Loader] for details.
- The `determineBuckets` method signature contains context variables as defined in the `providedContext`. These automatically get mapped to their corresponding variable names in the context mapping.
- `ProctorResult` encapsulates the collection of test-buckets identified for each test in an application's specification. The `ProctorResult` is a thin wrapper around a map from `testName => TestBucket`. This map will only contain tests that satisfy all of the following:
  - The `Identifiers` contain an value for the test's `test-type`. If a test-type is `RANDOM`, it will only be included if `Identifiers.randomEnabled` is `true`.
  - A test's `eligibility-rule` must have evaluated to `true`.
  - The test-matrix must contain a valid `test-definition` for the test.

### Generated ExampleGroups.java

```java
package com.indeed.example;

public class ExampleGroups extends AbstractGroups {
    public static final ExampleGroups EMPTY = new ExampleGroups(ProctorResult.EMPTY);

    public ExampleGroups(final ProctorResult proctorResult) {
        super(proctorResult);
    }

    public enum Test {
        BGCOLORTST("bgcolortst");
    }

    public enum Bgcolortst implements Bucket<Test> {
            INACTIVE(-1, "inactive"),
            ALTCOLOR1(0, "altcolor1"),
            ALTCOLOR2(1, "altcolor2"),
            ALTCOLOR3(2, "altcolor3"),
            ALTCOLOR4(3, "altcolor4");    
    }

    public Bgcolortst getBgcolortst();
    public int getBgcolortstValue(final int defaultValue);

    public boolean isBgcolortstInactive();
    public boolean isBgcolortstAltcolor1();
    public boolean isBgcolortstAltcolor2();
    public boolean isBgcolortstAltcolor3();
    public boolean isBgcolortstAltcolor4();
}
```

Key points

- `AbstractGroups` provides an application-specific meaning to each test and test-bucket.
- An `enum` is created for each `test` and corresponding `group`.
- Accessors for each test `group` are generated and can be used to check whether a proctor-result contains the specified `test bucket`.

### Generated ExampleGroups.js

<pre><code>define('com.indeed.example.groups', [], function() {

  var ExampleGroups_ = function(opt_values) {
    if (opt_values) {
      var testDef;
      testDef = opt_values[0];
      this.bgcolortstValue_ = testDef[0];
    } else {
      this.bgcolortstValue_ = -1;
    }
  };


  // BGCOLORTST

  ExampleGroups_.prototype.bgcolortstValue_;

  ExampleGroups_.prototype.isBgcolortstInactive = function() {
    return this.bgcolortstValue_ === -1;
  };

  ExampleGroups_.prototype.isBgcolortstAltcolor1 = function() {
    return this.bgcolortstValue_ === 0;
  };

  ExampleGroups_.prototype.isBgcolortstAltcolor2 = function() {
    return this.bgcolortstValue_ === 1;
  };

  ExampleGroups_.prototype.isBgcolortstAltcolor3 = function() {
    return this.bgcolortstValue_ === 2;
  };

  ExampleGroups_.prototype.isBgcolortstAltcolor4 = function() {
    return this.bgcolortstValue_ === 3;
  };


  var groups_ = null;

  return {

    init: function(values) {
      groups_ = new ExampleGroups_(values);
      return groups_;
    },

    getGroups: function() {
      if (groups_ == null) {
        groups_ = new ExampleGroups_();
      }
      return groups_;
    }

  };
});</code></pre>

Key points

- Functionality of JavaScript groups is dependent on using `determineBuckets` in your Java code.
- The value passed to `init()` is an array of bucket allocations and payloads. A method is included in `AbstractGroups` to generate this object.
- A `Google Closure` style generated file is also available.

## <a name="maven"></a>proctor-maven-plugin
The `proctor-maven-plugin` plugin makes it easy to incorporate Java code generation into the [maven build lifecycle](http://maven.apache.org/guides/introduction/introduction-to-the-lifecycle.html).

| Goal | Default Phase | Description |
| ---- | ------------- | ----------- |
| `generate` | generate-sources | Generates groups and groups-manager from `src/main/proctor` and adds specification as source resource |
| `generate-test` | generate-test-sources | Generates groups and groups-manager from `src/test/proctor` and adds specification as test resource |
| `generate-js` | generate-js-sources | Generates groups from `src/main/proctor` and adds specification as source resource |
| `generate-js-test` | generate-js-test-sources | Generates groups from `src/test/proctor` and adds specification as test resource |

The following `plugin` element should be added to your application's `pom.xml` ([complete pom.xml example](https://gist.github.com/parker/c0ea111ff343f58346e0#file-pom-xml)):

```xml
...
  <plugin>
    <groupId>com.indeed</groupId>
    <artifactId>proctor-maven-plugin</artifactId>
    <version>1.0-SNAPSHOT</version>
    <executions>
      <execution>
        <id>proctor-generate</id>
        <goals>
          <goal>generate</goal>
        </goals>
      </execution>
    </executions>
  </plugin>
...
```

The `generate` goal is executed in the standard compile and build lifecycle. To manually run the code generator, run the following in a terminal:

<pre><code>$ mvn com.indeed:proctor-maven-plugin:generate</code></pre>

By convention, the plugin determines the Java package and classname from the specification's path and filename, respectively:

<pre><code>.
├── src
|   ├── main
|       ├── proctor
|           ├── org/your/company/app/ExampleGroups.json
|   ├── test
|       ├── proctor
|           ├── org/your/company/app/ExampleGroups.json</code></pre>

<pre><code>src/main/org/your/company/app/ExampleGroups.json
    => org.your.company.app.ExampleGroups.java
    => org.your.company.app.ExampleGroupsManager.java</code></pre>

Alternatively, for a split specification, the format would look like this:

<pre><code>.
├── src
|   ├── main
|       ├── org/your/company/app/Example
|           ├── providedcontext.json
|           ├── examplefirsttest.json
|           ├── examplesecondtest.json
|           ├── examplethirdtest.json
|   ├── test
|       ├── org/your/company/app/Example
|           ├── providedcontext.json
|           ├── examplefirsttest.json
|           ├── examplesecondtest.json
|           ├── examplethirdtest.json</code></pre>

The generated file is sent to target/generated-resources/proctor and must be added separately in the pom.xml for inclusion in classpath resources:

```xml
  <build>
   ...
      <resources>
          <resource>
              <directory>${project.build.directory}/generated-resources/proctor</directory>
          </resource>
      </resources>
  </build>
```

## <a name="ant"></a>proctor-ant-plugin
The `proctor-ant-plugin` project provides two ant tasks that can be invoked during ant's build process: `com.indeed.proctor.consumer.gen.ant.TestGroupsJavaGeneratorTask` is used to generate Java code, `com.indeed.proctor.consumer.gen.ant.TestGroupsJavascriptGeneratorTask` is used to generate JavaScript code


1. Add a _proctor_ configuration and _proctor-ant-plugin_ dependency to your application's _ivy.xml_

   ```xml
    <configurations defaultconfmapping="default->default(master)">
        <conf name="compile" extends="default"/>
        <conf name="proctor" extends="compile"/>
    </configurations>

    <dependencies>
        <dependency org="com.indeed" name="proctor-ant-plugin"
                    rev="1.0-SNAPSHOT" conf="proctor->default" />
    </dependencies>
   ```

2. Create specification in your application's `src/resources` directory:

   <pre><code>    .
    ├── src
    |   ├── resources
    |       ├── org/your/company/app/ExampleGroups.json</code></pre>

   Alternatively, for a split specification, the format would look like this:

   <pre><code>  .
   ├── src
   |   ├── resources
   |       ├── org/your/company/app
   |           ├── providedcontext.json
   |           ├── examplefirsttest.json
   |           ├── examplesecondtest.json
   |           ├── examplethirdtest.json</code></pre>


3. Add a classpath ref for the _proctor_ configuration and define a ant target for invoking the task. Unlike the maven plugin, the package, groups class name, and groups manager class name must be specified in the ant task. Typically, the _compile_ target depends on the _proctor-generate_ target and the generated-code (in `generated-src`) is not committed to version-control.

   ```xml
    <target name="init" description="Resolve dependencies and set classpaths">
        ...
        <ivy:cachepath pathid="proctor.path"  conf="proctor"/>
        ...
    </target>

    <target name="proctor-generate-java" depends="init" description="generates the Java Proctor Groups and GroupsManager for the provided specification">
        <mkdir dir="generated-src/java" />
        <taskdef name="proctor-gen" classname="com.indeed.proctor.consumer.gen.ant.TestGroupsJavaGeneratorTask" classpathref="proctor.path" />
        <proctor-gen
                  input="src/resources/org/your/company/app/ExampleGroups.json"
                  target="generated-src/java"
                  packageName="org.your.company.app"
                  groupsClass="ExampleGroups"
                  groupsManagerClass="ExampleGroupsManager"/>
    </target>

    <target name="proctor-generate-js" depends="init" description="generates the JavaScript Proctor Groups for the provided specification">
        <taskdef name="proctor-js-gen" classname="com.indeed.proctor.consumer.gen.ant.TestGroupsJavascriptGeneratorTask" classpathref="proctor.path" />
        <proctor-js-gen
                input="src/resources/org/your/company/app/ExampleGroups.json"
                target="generated-src/js"
                packageName="org.your.company.app"
                groupsClass="ExampleGroups"
                useClosure="false"/>

        </target>

    <target name="compile" depends="proctor-generate-java">
      ...
    </target>

    <target name="makeweb" depends="proctor-generate-js">
          ...
    </target>
   ```

  See [example ivy.xml and build.xml gist](https://gist.github.com/parker/9eebd91fcb57ea416c6a) for a complete example.

  However, for a split specification the _proctor-gen_ task must be called with the input parameter as the containing folder of the JSONs, and with the extra parameter `specificationOutput` as the output:

  ```xml
        <proctor-gen
                  input="src/resources/org/your/company/app"
                  target="generated-src/java"
                  specificationOutput="generated-src/resources"
                  packageName="org.your.company.app"
                  groupsClass="ExampleGroups"
                  groupsManagerClass="ExampleGroupsManager"/>
  ```


[Loader]: {{ site.baseurl }}/docs/loader
