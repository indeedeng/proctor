---
layout: default
title: Loading Test Matrix
permalink: /docs/loader/
---

The _proctor loader_ can load the `test matrix` for an application. It ensures each test-definition is compatible with the application's specification. Create a single instance of a _proctor loader_ for a application to use when constructing its _AbstractGroupsManager_ generated class.

## AbstractProctorLoader
Proctor is bundled with several implementations of `AbstractProctorLoader` that can load the `test matrix`. You can implement a custom loader by extending `AbstractProctorLoader` or `AbstractJsonProctorLoader`.

**FileProctorLoader** - Loads the test matrix from a specified file. At Indeed, all applications use this implementation to load a file containing the test matrix managed by our operations team. See the [Deployment section][Deployment] for guidance about loading the `test matrix`.

**ClasspathProctorLoader** - Loads the test matrix from a classpath resource. Useful for writing unit-tests involving Proctor or deploying the test matrix alongside the application.

**UrlProctorLoader** - Loads the test from from a given URL.

**JsonProctorLoaderFactory** - Factory class for configuring `FileProctorLoader` or `ClasspathProctorLoader`.

Java example:

<pre><code>final JsonProctorLoaderFactory factory = new JsonProctorLoaderFactory();
// Loads the specification from the classpath resource
factory.setSpecificationResource("classpath:/org/your/company/app/ExampleGroups.json");
// Loads the test matrix from a file
factory.setFilePath("/var/local/proctor/test-matrix.json");
final AbstractJsonProctorLoader loader = factory.getLoader();</code></pre>

Spring configuration XML example:

```xml
<bean id="loaderFactory" class="com.indeed.proctor.common.JsonProctorLoaderFactory">
    <!-- Loads the specification from the classpath resource -->
    <property name="specificationResource" value="classpath:/org/your/company/app/ExampleGroups.json" />
    <!-- Loads the test matrix from a file -->
    <property name="classResourcePath" value="/var/local/proctor/test-matrix.json" />
</bean>
<bean id="proctorLoader" factory-bean="loaderFactory" factory-method="getLoader" />
```

The `JsonProctorLoaderFactory` also supports loading the specification from the file system and test-matrix from the classpath. Both uses are rare but can be useful if you want to bundle the test-matrix with your application and don't need to update the test-matrix without deploying new code.

```xml
<bean id="loaderFactory" class="com.indeed.proctor.common.JsonProctorLoaderFactory">
    <!-- Load the specification resource from the file-system (not common) -->
    <property name="specificationResource" value="WEB-INF/org/your/company/app/ExampleGroups.json" />
    <!-- Load the test matrix from a classpath resource (not common) -->
    <property name="classResourcePath" value="/proctor/test-matrix.json" />
</bean>
<bean id="proctorLoader" factory-bean="loaderFactory" factory-method="getLoader" />
```

##  Load-verify Loop
The Proctor loader periodically refreshes the `test matrix` and performs the following steps during each attempt:

1. Reads and parses the `test matrix` from a JSON file, classpath or URL depending on the implementation of `AbstractJsonProctorLoader`. Invalid JSON preventing the deserialization of the `test matrix` will result in a failed recent attempt.
2. Checks the `audit.version` of the new test-matrix against the `audit.version` of last successful refresh. If these values are equal, the attempt is finished and no further steps are executed.
3. Validates each test in the `specification` against the corresponding `test-definition` in the `test matrix`:
   1. A test-definition should be defined for each test. A matrix missing a test is invalid.
   2. A test-definition allocating users to a bucket not defined in the specification is invalid. It's valid for the test-definition to define new buckets in its `buckets` field, but their group size should be 0% across all test allocations.
   3. A test-definition must have valid payloads. If the application expects a test to have payloads, each bucket in the test-definition must have a payload of the correct type and pass the `payload-validator` expression. If an application doesn't expect a test to have payloads, no validation of their values is performed.
   4. A test-definition's allocations each must sum to 1.0.
   5. A test-definition must have exactly one allocation with a null or empty rule. This allocation must be the last allocation in the `allocations` array.

4. Creates a new `Proctor` instance containing the data from the loaded `test matrix`. The instance is considered the last successful test-matrix.

Refer to [Inspecting the Loader State](#inspecting) for details about determining whether the loader has recently refreshed successfully.

## Scheduling the Loader
The `AbstractProctorLoader` extends `java.util.TimerTask` and must be scheduled to run periodically.

The following examples show how to schedule the loader to refresh every 30 seconds. Each example creates a new ScheduledExecutorService specifically for the `AbstractJsonProctorLoader`. An application can reuse another ScheduledExecutorService if it already uses one for background tasks.

Java example:

<pre><code>...
final AbstractJsonProctorLoader loader = factory.getLoader();

final ScheduledExecutorService scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
scheduledExecutorService.scheduleWithFixedDelay(loader, 0, 30, TimeUnit.SECONDS);</code></pre>

Spring XML example:

```XML
...
<bean id="proctorLoader" factory-bean="loaderFactory" factory-method="getLoader" />
<bean id="scheduledExecutorService" class="org.springframework.scheduling.concurrent.ScheduledExecutorFactoryBean" lazy-init="false">
    <property name="threadNamePrefix" value="BackgroundTasks"/>
    <property name="continueScheduledExecutionAfterException" value="true"/>
    <property name="poolSize" value="1"/>
    <!-- Add all of the scheduled Tasks to the Executor Service -->
    <property name="scheduledExecutorTasks" >
        <list>
            <!-- scheduledExecutorService.scheduleWithFixedDelay(proctorLoader, 0, 30, TimeUnit.SECONDS); -->
            <bean class="org.springframework.scheduling.concurrent.ScheduledExecutorTask">
                <property name="runnable" ref="proctorSupplier" />
                <property name="delay" value="0" />
                <property name="period" value="30000" />
                <property name="fixedRate" value="false" />
            </bean>
        </list>
    </property>
</bean>
```

## <a name="inspecting"></a>Inspecting the Loader State
Depending on the outcome of the `load-verify loop`, the loader may be in one of these states:

`UNLOADED`: The loader has never successfully loaded the `test matrix`. This state occurs when the `ScheduledExecutorService` has not yet run the loader, or when the JSON file could not be read or parsed.

`LOADED-COMPLETE`: The most recent refresh was successful and did not have verification problems.

`LOADED-PARTIAL`: The most recent refresh parsed the `test matrix` successfully, but verification errors occurred in at least one `test definition`.

`LOADED-STALE`: The most recent refresh was a failure: The `test matrix` could not be read or parsed. The loader will use test definitions from a previous successful attempt.

You can interpret the loader's state from a combination of the following _AbstractProctorLoader_ and _Proctor_ methods:

| Method | Description |
| ------ | ---------- |
| `boolean isLoadedDataSuccessfullyRecently()` | Returns flag whose value indicates whether the most recent refresh was successful. <br/>A `true` value indicates the loader is in the `LOADED-COMPLETE` or `LOADED-PARTIAL` state.  |
| `Proctor loader.get()` | Returns the currently loaded Proctor instance. <br/>This method will return `null` when in the `UNLOADED` state. |
| `ProctorLoadResult Proctor.getLoadResult()` | Returns a ProctorLoadResult instance describing the outcome of the most recent load. <br/> `ProctorLoadResult.hasInvalidTests` can be used to differentiate between the `LOADED-COMPLETE` and `LOADED-PARTIAL` states. |


<pre><code>private enum ProctorRefreshState {
  UNLOADED,LOADED_COMPLETE,LOADED_PARTIAL,LOADED_STALE;
}

private ProctorRefreshState determineState(final AbstractProctorLoader loader) {
  final Proctor proctor = loader.get();
  final boolean recentSuccess = loader.isLoadedDataSuccessfullyRecently();

  final ProctorRefreshState state;
  if(recentSuccess) {
    // LOADED-COMPLETE or LOADED-PARTIAL
    assert proctor != null;
    final ProctorLoadResult loadResult = proctor.getLoadResult();
    state = loadResult.hasInvalidTests() ? ProctorRefreshState.LOADED_PARTIAL : ProctorRefreshState.LOADED_COMPLETE;
  } else {
    // loader.getLastLoadErrorMessage() will contain the root cause of the failed attempt

    if(proctor == null) {
      state = ProctorRefreshState.UNLOADED;
    } else {
      state = ProctorRefreshState.LOADED_STALE;
      // similar to above, the ProctorLoadResult can be inspected to determine if the previous test matrix had any invalid test definitions
    }
  }
  return state;
}</code></pre>

## Exporting the Loader State
The `AbstractProctorLoader` extends [com.indeed.util.core.DataLoadingTimerTask][GithubUtil] and uses [VarExport][GithubUtilVarexport] to make its state available for debugging. The _com.indeed.util.varexport.servlet.ViewExportedVariablesServlet_ can be used to view the exported namespaces and variables ([example web.xml][proctor-demo-web.xml]).

| Namespace and variable | Description |
| ---------------------- | ----------- |
| `FileProctorLoader.last-audit` | JSON representation of _com.indeed.proctor.common.model.Audit_ containing matrix version information |
| `FileProctorLoader.last-error` | String containing any error message from the most recent refresh |
| `ProctorLoaderDetail.file-source` | The source of the loader |
| `ProctorLoaderDetail.file-contents` | The raw contents of the loaded source file |

`Note`: The _FileProctorLoader_ namespace is the loader class name, which varies depending on your application's `AbstractProctorLoader` implementation.


[Inspecting]: #toc_5
[GithubUtil]: http://www.github.com/indeedeng/util
[GithubUtilVarexport]: http://www.github.com/indeedeng/util/tree/master/varexport
[proctor-demo-web.xml]: http://www.github.com/indeedeng/proctor-demo/tree/master/src/main/webapp/WEB-INF/web.xml
[Deployment]: {{ site.baseurl }}/docs/deploying-groups/
