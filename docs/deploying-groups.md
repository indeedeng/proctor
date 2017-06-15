---
layout: default
title: Deploying
permalink: /docs/deploying-groups/
---

# Order of Operations

When creating, modifying, or removing a test, it's critical to deploy the changes in the order specified below. This  ensures a consistent user experience.

## Creating a Test

When you create a test, you must deploy your test matrix before your application. Since your application expects the test to exist, it will issue a warning on startup if the test matrix does not include the new test definition. All users will be placed in the inactive test group until the test definition exists.

## Removing a Test

To remove a test, first remove the test from the Proctor specification in your application. After your application is deployed, you can safely remove the test from the test matrix.

## Adding a Bucket

You can add a bucket in any order, as long as the new bucket is set to 0%. You must add the bucket to your application's Proctor specification before you increase the bucket's percentage.

## Removing a Bucket

Remove the bucket from the test matrix or set it to 0% before you remove it from your application's Proctor specification.
