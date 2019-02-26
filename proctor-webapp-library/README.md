# Proctor

Proctor-Webapp-Library is a library used for running a web application to create and modify [Proctor](https://github.com/indeedeng/proctor) definitions.

# Features:

# Installation

# Example
See [proctor-webapp](http://www.github.com/indeedeng/proctor-webapp)

# Documentation

Proctor-Webapp-Library assumes that users will have a trunk/qa/prod lifecycle for test definitions. Users can define tests in trunk, and promote them to qa, then prod.
Users can modify the promotion behavior by implementing interfaces in com.indeed.proctor.webapp.extensions as spring beans. 

## Backend configuration

Proctor-webapp-library has a bean RevisionControlStoreFactory which expects several properties to be set, defining the storage of the test matrix.

* `revision.control` whether to use git or svn
* `scm.path` location of the repository

Based on this, 3 test matrix stores will be created based on applicationContext.xml:

* TRUNK
* QA
* PROD


# License

[Apache License Version 2.0](https://github.com/indeedeng/proctor-webapp-library/blob/master/LICENSE)

