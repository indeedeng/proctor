---
layout: default
title: Maven Modules
permalink: /docs/maven-modules/
---

## proctor-common
The core classes responsible for allocating users to groups, evaluating test rules, and loading the test-matrix.

{% include maven_dependency.md artifact="proctor-common" %}

## proctor-consumer
Provides the `AbstractGroups` and `AbstractGroupsManager` classes to use when referencing groups in application code. The `AbstractShowTestGroupsController` and `ViewProctorSpecificationServlet` web servlets can be used to display the current state of Proctor loaded by the application.

{% include maven_dependency.md artifact="proctor-consumer" %}

## proctor-codegen
A code generator that can be used to generate an application's `AbstractGroups` and `AbstractGroupsMananger` implementations in Java based on an application's test _specification_.

{% include maven_dependency.md artifact="proctor-codegen" %}

## proctor-maven-plugin
A maven plugin that invokes the Java and JavaScript code generators from `proctor-codegen`.

{% include maven_dependency.md artifact="proctor-maven-plugin" %}

## proctor-ant-plugin
An ant task that invokes the Java and JavaScript code generators from `proctor-codegen`.

{% include maven_dependency.md artifact="proctor-ant-plugin" %}

{% comment %}include build.xml snippet {% endcomment %}

## proctor-builder

{% include maven_dependency.md artifact="proctor-builder" %}


## proctor-store

{% include maven_dependency.md artifact="proctor-store" %}

## proctor-store-svn

{% include maven_dependency.md artifact="proctor-store-svn" %}
