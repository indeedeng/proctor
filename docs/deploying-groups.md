---
layout: default
title: Deploying
permalink: /docs/deploying-groups/
---

# Order of Operations (deploys)

When creating, modifying, or removing a test, the order in which your application and test matrix are deploy is important to ensure that your users continue to see an consistent experience.

## Creating new Test

Creating a new test requires that your test matrix be deployed before your application. Since your application is expecting the test to exist, it will throw an warning on startup if the test matrix does not include the new test definition. All users will be placed in the inactive test group until the test definition exists.

## Removing a test

To remove a test, first remove the test from the proctor specification in your application. After your application is deployed, then you can safely remove the test from the test matrix.

## Adding a bucket

Adding a bucket can be done in any order, as long as the new bucket is set to 0%. Before increasing the percentage of a bucket, your application's proctor specification must be aware of it.

## Removing a bucket

The bucket must be removed from the test matrix or be set to 0% before removing it from your application's proctor specification.
