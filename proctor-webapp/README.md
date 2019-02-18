# Proctor
Proctor-Webapp is a Java web application that uses the [Proctor-Webapp-Library](https://github.com/indeedeng/proctor-webapp-library) to manipulate and view Proctor definitions.

# Features:
- Create/View/Edit/Delete Proctor definitions
- View commit history of Proctor definitions in trunk, qa, and production.
- Promote Proctor definitions from trunk -> qa/production, or qa -> production

# Test-matrix storage requirements


[Proctor test-matrix](http://indeedeng.github.io/proctor/docs/matrix-schema/) must be accessible for trunk, qa, and production:

For SVN:
    * https://YOURSVN.com/svn/repos/proctor-data/trunk/matrices/test-definitions/
    * https://YOURSVN.com/svn/repos/proctor-data/branches/deploy/qa/matrices/test-definitions/
    * https://YOURSVN.com/svn/repos/proctor-data/branches/deploy/production/matrices/test-definitions/

For git:
    * TODO: Create local repository with trunk/qa/prod branches and matrices root folder on each branch...

# Deployment
1. Use git to clone https://github.com/indeedeng/proctor-webapp
2. Set up configuration (see below).
3. In root folder, run `mvn package -pl proctor-webapp -am` to build.
4. Start the webapp runner. NOTE: The **config.dir** java variable is set to the example-apache-config, you will still need to update **proctor-config.properties** with the appropriate credentials:

    ```bash
    java -Dconfig.dir="$PWD/example-apache-config"  -jar target/dependency/webapp-runner.jar --context-xml example-apache-config/proctor-webapp.xml --expand-war target/proctor-webapp-1.0.0-SNAPSHOT.war
    ```

# Configuration
Two configuration files need to be provided to run Proctor-Webapp properly. The properties have the recommended values below.

1. **${config.dir}/proctor-config.properties**

When using SVN as storage:

    ```bash
    revision.control=svn
    scm.path=https://YOURSVN.com/svn/repos/proctor-data/
    scm.login=REPLACE_WITH_USER
    scm.password=REPLACE_WITH_PASS
    ```

When using Git as storage:

    ```bash
    revision.control=git
    # TODO
    ```

2. **${config.dir}/proctor-webapp.xml**
    ```
    <Context debug="5" reloadable="true" crossContext="true">
        <Parameter name="contextConfigLocation" value="/WEB-INF/spring/applicationContext.xml" override="false"/>
        <Parameter name="propertyPlaceholderResourceLocation" value="file://${config.dir}/proctor-config.properties" override="false"/>
    </Context>
    ```
