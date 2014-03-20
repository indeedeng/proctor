# Proctor
Proctor-Webapp-Public is a Java web application that uses the [Proctor-Webapp-Library](https://github.com/indeedeng/proctor-webapp-library) to manipulate and view Proctor definitions.

# Features:
- Create/View/Edit/Delete Proctor definitions
- View commit history of Proctor definitions in trunk, qa, and production.
- Promote Proctor definitions from trunk -> qa/production, or qa -> production

# Requirements
- [Proctor test-matrix](http://indeedeng.github.io/proctor/docs/matrix-schema/) must be accessible through SVN for trunk, qa, and production:
    * https://YOURSVN.com/svn/repos/proctor-data/trunk/matrices/test-definitions/
    * https://YOURSVN.com/svn/repos/proctor-data/branches/deploy/qa/matrices/test-definitions/
    * https://YOURSVN.com/svn/repos/proctor-data/branches/deploy/production/matrices/test-definitions/

# Deployment
1. Use git to clone https://github.com/indeedeng/proctor-webapp-public, and run mvn package to build.
3. Set up configuration (see below).
4. Start the webapp runner. NOTE: The **config.dir** java variable is set to the example-apache-config, you will still need to update **proctor-config.properties** with the appropriate credentials:
    ```bash
    $ java -Dconfig.dir="$PWD/example-apache-config" \
           -jar target/dependency/webapp-runner.jar \
           --context-xml example-apache-config/proctor-webapp.xml \
           --expand-war target/proctor-webapp-public-1.0.0-SNAPSHOT.war \
    ```

# Configuration
Two configuration files need to be provided to run Proctor-Webapp-Public properly. The properties have the recommended values below.

1. **${config.dir}/proctor-config.properties**

    ```bash
    svn.path=https://YOURSVN.com/svn/repos/proctor-data/
    svn.login=REPLACE_WITH_USER
    svn.password=REPLACE_WITH_PASS
    ```

2. **${config.dir}/proctor-webapp.xml**
    ```
    <Context debug="5" reloadable="true" crossContext="true">
        <Parameter name="contextConfigLocation" value="/WEB-INF/spring/applicationContext.xml" override="false"/>
        <Parameter name="propertyPlaceholderResourceLocation" value="file://${config.dir}/proctor-config.properties" override="false"/>
    </Context>
    ```

# Discussion

Join the [indeedeng-proctor-users](https://groups.google.com/d/forum/indeedeng-proctor-users) mailing list to ask questions and discuss use of Proctor-Webapp and/or Proctor.

# Contributing

# License

[Apache License Version 2.0](https://github.com/indeedeng/proctor-webapp-public/blob/master/LICENSE)