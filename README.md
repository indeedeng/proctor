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
1. Use git to clone https://github.com/indeedeng/proctor-webapp-public, and run mvn install to build.
2. Copy the .war to your tomcat webapps directory.
3. Set up configuration (see below)
4. Run tomcat

# Configuration
Several configuration files need to be provided to run Proctor-Webapp-Public properly. The properties have the recommended values below. Be sure to replace **${catalina.base}** with the location of your tomcat.

1. **${catalina.base}/conf/dev-config.properties**

    ```bash
    verify.http.timeout=1000
    verify.executor.threads=10
    use.compiled.css=true
    use.compiled.javascript=true
    revision.control=svn
    revision.control.configuration.file=${catalina.base}/conf/proctor-webapp-config.properties
    ```

2. **${catalina.base}/conf/proctor-webapp-config.properties**

    ```bash
    svn.path=https://YOURSVN.com/svn/repos/proctor-data/
    svn.login=REPLACE_WITH_USER
    svn.password=REPLACE_WITH_PASS

    #optional
    svn.tempdir.max.age.minutes=1440
    svn.refresh.period.minutes=5
    svn.cache=true
    ```

The follow context parameters must also be set in your tomcat configuration:
    ```
    <Parameter name="contextConfigLocation" value="/WEB-INF/spring/applicationContext.xml"/>
    <Parameter name="propertyPlaceholderResourceLocation" value="file:${catalina.base}/conf/dev-config.properties"/>
    ```

# Discussion

Join the [indeedeng-proctor-users](https://groups.google.com/d/forum/indeedeng-proctor-users) mailing list to ask questions and discuss use of Proctor-Webapp and/or Proctor.

# Contributing

# License

[Apache License Version 2.0](https://github.com/indeedeng/proctor-webapp-public/blob/master/LICENSE)