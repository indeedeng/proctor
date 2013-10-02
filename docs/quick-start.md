---
layout: default
title: Quick Start
permalink: /docs/quick-start/
---

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

Use git to clone https://github.com/indeedeng/proctor, and run `mvn install` to build.

## Implement an A/B test with Proctor

Let's say you want to do an A/B test of a new background color for your web application.
This example is similar to the one in the https://github.com/indeedeng/proctor-demo reference implementation project.
It uses maven to build and the Proctor maven plugin to generate Proctor convenience classes.

### Specify the test

The first step is write the JSON specification for your test. This should go into a Java-like package structure under
`src/main/proctor/`. Let's say your desired package name is `org.example.proctor` and you want to have use `ExampleGroups`
in generated Java class names. Create the file `src/main/proctor/org/example/proctor/ExampleGroups.json` with this content:

{% gist parker/3bb0e94b9b238b48429f 0-ExampleGroups.json %}
Now you have a single test `bgcolortst` with five buckets: `inactive, altcolor1, altcolor2, altcolor3, altcolor4`.

### Set up the maven plugin and generate code

Edit your `pom.xml` to enable the Proctor maven plugin:

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

To generate the Proctor convenience classes, run `mvn proctor:generate`. That will create `ExampleGroups.java` and `ExampleGroupsManager.java`
in `target/generated-sources/proctor/org/example/proctor/`.

### Create your initial test definition

In the test specification you created above, you gave yourself 4 test buckets to work with, in addition to the inactive bucket. Let's say
you want to put 10% of your users in each test bucket. In order to allow you to grow the buckets later without moving users between buckets,
you'll want to leave space between them, so your definition should be:

{% include range_allocation_table.html buckets='altcolor1 inactive altcolor2 inactive altcolor3 inactive altcolor4' values="0 -1 1 -1 2 -1 3" ranges='10 20 10 20 10 20 10' %}



Create a file called `proctor-definition.json` with this content:

{% gist youknowjack/6782462 proctor-definition.json %}

You'll be loading this definition from the file system as your test matrix.

### Write some code

First you'll need to load your specification (from the classpath) and use it to load your test matrix.

{% gist youknowjack/6782938 LoadProctor.java %}

Now that you have the Proctor object, you can use it to get the generated convenience class objects.  Note that you'll need to provide
some kind of user unique id (typically stored in a cookie for web applications), since this is a `USER` test.


{% gist youknowjack/6783121 GetProctorGroups.java %}

You can now use the ExampleGroups object in your Java code or your view templates.

#### Java example

{% gist youknowjack/6783207 ExampleGroupsUsage.java %}

#### JSP example

{% gist youknowjack/6783315 %}

### Test out your groups

You can easily test the different buckets by appending the query param `prforceGroups`. For example, `?prforceGroups=bgcolortst1` would
temporarily put you into bucket 1 of the `bgcolortst` test, and would set a `prforceGroups` cookie to keep you in that group for the length
of your browser session.
