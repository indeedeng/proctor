---
layout: default
title: Schema
permalink: /docs/matrix-schema/
---

The `test matrix` has two on-disk representations: 

- a compiled single-file 
- uncompiled file per test-definition. 

The single-file variant is compiled using the [proctor builder][builder] and loaded by applications via the [proctor loader][loader] implementations. The single-file schema contains additional information about the when the file was generated and what version it represents.


## single-file JSON Schema
| JSON Property | Description |
| ------------- | ----------- |
| `tests` | collection of test-definitions. A map of `testName => test-definition`, see [test-definition schema][test-definition] for the expected format |
| `audit` | metadata about the test matrix |
| `audit.version` | version number for the test matrix. At Indeed, the test-definitions are stored in SVN and the `version` value is the subversion revision from which the matrix was compiled |
| `audit.updatedBy` | String indicating who was responsible for compiling the matrix. At Indeed, the builder is initiated by Jenkins and the `updatedBy` is set as the jenkins-job id |
| `audit.updated` |  timestamp of the generated file |

## file per test-definition
In its uncompiled format, the test-matrix is stored as a single file per [test-definition][test-definition]. 
In the _test-definitions_ directory, each test must have a corresponding directory and _definition.json_ file. 
The directory name dictates the name of the test and the _definition.json_ file describes the buckets, allocations and other attributes of the test definition.

```bash
.
├── test-definitions
|   ├── appfeatureatst
|       ├── definition.json
|   ├── appfeaturebtst
|       ├── definition.json
|   ├── otherappfeaturetst
|       ├── definition.json
```

The above example would describe a test matrix with three tests: `appfeaturetst, appserptst, otherappfeaturetst`

## Test naming conventions

If you're using Proctor across multiple applications, it's advisable to use standard test naming convention to help differentiate which tests are for each application.

Something of the following has worked for Indeed: `[application-name][feature-name]tst`

`application-name`: a short-name of your application: a product code-name or jira-project

`feature-name`: a description of the feature impacted by the test

[test-definition]: {{ site.baseurl }}/docs/test-definition/
[loader]: {{ site.baseurl }}/docs/loader/
[builder]: {{ site.baseurl }}/docs/builder/