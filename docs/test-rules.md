---
layout: default
title: Test Rules
permalink: /docs/test-rules/
---
A test may contain two types of rules: [eligibility rules]({{ site.baseurl }}/docs/terminology/#eligibility-rule) and [allocation rules]({{ site.baseurl }}/docs/terminology/#allocation-rule).


## Rule Format
All rules must be written in [Unified Expression Language](http://en.wikipedia.org/wiki/Unified_Expression_Language), and must return a boolean value.
Test rules may be set to `null` or literal values. Test rules that are set to `null`, an empty rule `"${}"`, `"${true}"`, or `"${TRUE}"` will evaluate to `true`.
Test rules that are set to `"${false}"` or `"${FALSE}"` will evaluate to `false`.


## Variables
Test rules can use any variables in the specification's [providedContext]({{ site.baseurl }}/docs/terminology/#context), [test constants]({{ site.baseurl }}/docs/terminology/#test-constants), or [special constants]({{ site.baseurl }}/docs/terminology/#special-constants).
Special constants set in a test definition can modify a test rule in the test-matrix. For example, if `__COUNTRIES` is set in `specialConstants`, the test-matrix will add `proctor:contains(__COUNTRIES, country)` to the rule for that test.
The following two JSON files show the change made from the test definition to the test-matrix.

Example Test Definition:
{%gist chriscolon/eb01453d5b5fcae7d655 2-ExampleTestSpecialConstants.json %}

Example Test Matrix With Modified Rule:
{%gist chriscolon/3fd535a0716dbed9055c 2-ExampleTestMatrixModifiedRule.json %}


### Rules Using Variables
The following example shows a rule that indicates a test should only be applied when the language is English, the country is US, and the test constant variable `id` is greater than 1000.
{% gist chriscolon/3a98e6e777bc4a7ca851 0-RuleFormat.json %}

## Namespace Function Libraries
Test rules have two namespace function libraries available to them by default:

| Namespace | Implementation Class | Notes |
| --------- | -------------------- | ------ |
| fn | `org.apache.taglibs.standard.functions.Functions` |  JSP EL functions from the [standard tag library](http://docs.oracle.com/javaee/5/jstl/1.1/docs/tlddocs/fn/tld-summary.html) |
| proctor | `com.indeed.proctor.common.ProctorRuleFunctions` | [Proctor-specific functions for rules](https://github.com/indeedeng/proctor/tree/master/proctor-common/src/main/java/com/indeed/proctor/common/ProctorRuleFunctions.java) |


### Rules Containing Namespace Functions
Test rules may take advantage of the two namespace function libraries available to them. In the following example, the eligibility rule uses the "contains" function from the "proctor" namespace.
{% gist chriscolon/c8472ecd726e0193006f 0-ProctorContainsFunction.json %}


### Extending Default Namespace Function Libraries

To add to the default namespace function libraries, Proctor provides users with a static function that generates a [LibraryFunctionMapperBuilder](https://github.com/indeedeng/proctor/tree/master/proctor-common/src/main/java/com/indeed/proctor/common/el/LibraryFunctionMapperBuilder.java)
that contains the default `fn` and `proctor` namespaces. Add the namespace and class to this builder and build.
Building will return a FunctionMapper containing the default namespaces as well as any that were added, as shown in the following code example.

<pre><code>final JsonProctorLoaderFactory factory = new JsonProctorLoaderFactory();
// Loads the specification from the classpath resource
factory.setSpecificationResource("classpath:/org/your/company/app/ExampleGroups.json");
// Loads the test matrix from a file
factory.setFilePath("/var/local/proctor/test-matrix.json");

// A custom FunctionMapper without the default namespaces fn and proctor
final FunctionMapper myFunctionMapper = new LibraryFunctionMapperBuilder()
                                            .add("namespace1", Class1.class)
                                            .add("namespace2", Class2.class)
                                        .build();

// A custom FunctionMapper WITH the default namespaces fn and proctor
final FunctionMapper extendedFunctionMapper = RuleEvaluator.defaultFunctionMapperBuilder()
                                              .add("namespace1", Class1.class)
                                              .add("Namespace2", Class2.class)
                                          .build();

factory.setFunctionMapper(myFunctionMapper); // or pass extendedFunctionMapper

final AbstractJsonProctorLoader loader = factory.getLoader();</code></pre>



----------------------------------------------------------------------------
