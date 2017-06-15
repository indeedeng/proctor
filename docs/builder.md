---
layout: default
title: Builder
permalink: /docs/builder/
---

The builder converts the multi-file representation of the test-matrix and generates a single-file containing all of the definition.json files for each of the tests. The builder will ensure that the test-definition files are internally consistent and do not refer to undefined bucket values or have allocations that do not sum to 1.0.

The multi-file format of the test-matrix has the following schema underneath a `root-directory`:

<pre><code>.
├── test-definitions
|   ├── appfeatureatst
|       ├── definition.json
|   ├── appfeaturebtst
|       ├── definition.json
|   ├── otherappfeaturetst
|       ├── definition.json
</code></pre>

## Usage
{% include maven_dependency.md artifact="proctor-builder" %}

<pre><code>$ java -cp xxx com.indeed.proctor.builder.LocalProctorBuilder -i /path/to/root-directory -o build/proctor --author $USER
</code></pre>

## Command-line arguments

| Argument | description |
| -------- | ----------- |
| `-i, --input` | The `root-directory` containing the `test-defintions` directory |
| `-o, --output` | The output directory. Defaults to '-', std-out |
| `-f, --filename` | The output filename. defaults to 'proctor-tests-matrix.json' |
| `-a, --author` | The value to use for `audit.updatedBy` |
| `-v, --version` | The version to use for `audit.version`. Supports subversion-like revisions using the 'r149569' format |
