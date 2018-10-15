---
layout: default
title: Quick Start
permalink: /docs/quick-start/
---
This section guides you through the process of getting Proctor and implementing a simple A/B test.

## Get Proctor

### Pulling from maven repository

Add these dependencies to your pom.xml:

```xml
    <dependencies>
        <!-- use this dependency only if your environment is providing tomcat libraries
        <dependency>
            <groupId>com.indeed</groupId>
            <artifactId>proctor-tomcat-deps-provided</artifactId>
            <version>1.0</version>
            <type>pom</type>
        </dependency>
        -->
        <dependency>
            <groupId>com.indeed</groupId>
            <artifactId>proctor-common</artifactId>
            <version>1.0</version>
        </dependency>
        <dependency>
            <groupId>com.indeed</groupId>
            <artifactId>proctor-consumer</artifactId>
            <version>1.0</version>
        </dependency>
        <dependency>
            <groupId>com.indeed</groupId>
            <artifactId>proctor-codegen</artifactId>
            <version>1.0</version>
        </dependency>
        <dependency>
            <groupId>com.indeed</groupId>
            <artifactId>proctor-maven-plugin</artifactId>
            <version>1.0</version>
        </dependency>
        <dependency>
            <groupId>com.indeed</groupId>
            <artifactId>util-core</artifactId>
            <version>1.0</version>
        </dependency>
    </dependencies>
```

### Building from source (using maven)

Use git to clone [https://github.com/indeedeng/proctor](https://github.com/indeedeng/proctor), and run `mvn install` to build.

## Implement an A/B test with Proctor

For this process, we'll use an example similar to the  [https://github.com/indeedeng/proctor-demo](https://github.com/indeedeng/proctor-demo) reference implementation project. You'll run an A/B test of a new background color for your web application. The example uses maven to build and the Proctor maven plugin to generate Proctor convenience classes.

### Specifying the test

1. Write the JSON specification for your test and put it into a Java-like package structure under
`src/main/proctor/`. For this example, name the package `org.example.proctor` and use `ExampleGroups`
in generated Java class names.
2. Create the file `src/main/proctor/org/example/proctor/ExampleGroups.json` with this content:

```{% gist parker/3bb0e94b9b238b48429f 0-ExampleGroups.json %}```

The result is a single test `bgcolortst` with five buckets: `inactive, altcolor1, altcolor2, altcolor3, altcolor4`.

### Setting up the maven plugin and generate code

1. Edit your `pom.xml` to enable the Proctor maven plugin:

    ```xml
    <build>
        <plugins>
          <!-- add this plugin definition -->
          <plugin>
            <groupId>com.indeed</groupId>
            <artifactId>proctor-maven-plugin</artifactId>
            <version>1.0</version>
            <executions>
              <execution>
                <id>proctor-generate</id>
                <goals>
                  <goal>generate</goal>
                </goals>
              </execution>
            </executions>
          </plugin>
        </plugins>
    </build>
    ```

2. To generate the Proctor convenience classes, run `mvn proctor:generate`, which creates `ExampleGroups.java` and `ExampleGroupsManager.java` in `target/generated-sources/proctor/org/example/proctor/`.

### Creating your initial test definition

In the test specification you created, you set up 4 test buckets and one inactive bucket. For this example, you'll put 10% of your users in each test bucket. Leaving space between each bucket allows you to increase them later without moving users between buckets. Your definition would be:

{% include range_allocation_table.html buckets='altcolor1 inactive altcolor2 inactive altcolor3 inactive altcolor4' values="0 -1 1 -1 2 -1 3" ranges='10 20 10 20 10 20 10' %}


1. Create a file called `proctor-definition.json` with this content:

   ```{% gist youknowjack/6782462 proctor-definition.json %}```

2. Load this definition from the file system as your test matrix.

### Writing some code

1. Load your specification (from the classpath) and use it to load your test matrix.

 ```  {% gist youknowjack/6782938 LoadProctor.java %}```

2. Use the Proctor object to get the generated convenience class objects. You'll need to provide a user unique id, typically stored in a cookie for web applications, since this is a `USER` test.


   ```{% gist youknowjack/6783121 GetProctorGroups.java %}```

You can now use the ExampleGroups object in your Java code or your view templates.

#### Java example

```{% gist youknowjack/6783207 ExampleGroupsUsage.java %}```

#### JSP example

```{% gist youknowjack/6783315 %}```

### Testing your groups

Test the different buckets by appending the query param `prforceGroups`. For example, `?prforceGroups=bgcolortst1` would temporarily put you into bucket 1 of the `bgcolortst` test, and would set a `prforceGroups` cookie to keep you in that group for the length of your browser session.
