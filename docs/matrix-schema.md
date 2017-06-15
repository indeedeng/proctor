---
layout: default
title: Schema
permalink: /docs/matrix-schema/
---

The `test matrix` has two on-disk representations:

- A compiled single-file
- Uncompiled file per test-definition

The single-file variant is compiled using the [Proctor builder][builder] and loaded by applications via the [Proctor loader][loader] implementations. The single-file schema contains information about when the file was generated and which version it represents.


## Single-file JSON Schema

| JSON Property | Description |
| ------------- | ----------- |
| `tests` | Collection of test-definitions. A map of `testName => test-definition`, see [test-definition schema][test-definition] for the expected format. |
| `audit` | Metadata about the test matrix. |
| `audit.version` | Version number for the test matrix. At Indeed, the test-definitions are stored in SVN and the `version` value is the Subversion revision from which the matrix was compiled. |
| `audit.updatedBy` | String indicating who was responsible for compiling the matrix. At Indeed, the builder is initiated by Jenkins and the `updatedBy` is set as the Jenkins job id. |
| `audit.updated` |  Timestamp of the generated file. |

## File per test-definition
In its uncompiled format, the test-matrix is stored as a single file per [test-definition][test-definition].
In the _test-definitions_ directory, each test must have a corresponding directory and _definition.json_ file.
The directory name dictates the test name and the _definition.json_ file describes the buckets, allocations, and other attributes of the test definition.

<pre><code>.
├── test-definitions
|   ├── appfeatureatst
|       ├── definition.json
|   ├── appfeaturebtst
|       ├── definition.json
|   ├── otherappfeaturetst
|       ├── definition.json
</code></pre>

The above example would describe a test matrix with three tests: `appfeaturetst, appserptst, otherappfeaturetst`

## Test Naming Conventions

If you're using Proctor across multiple applications, it's advisable to use standard test naming conventions to help differentiate the tests you use for each application.

Indeed has used a convention similar to this example: `[application-name][feature-name]tst`

`application-name`: A short name you use for your application, such as a product codename or JIRA project.

`feature-name`: Description of the feature impacted by the test.

[test-definition]: {{ site.baseurl }}/docs/test-definition/
[loader]: {{ site.baseurl }}/docs/loader/
[builder]: {{ site.baseurl }}/docs/builder/
