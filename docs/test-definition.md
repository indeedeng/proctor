---
layout: default
title: Test Definition
permalink: /docs/test-definition/
---

The test definition describes a test's buckets, allocations, eligibility rules, allocations and constants. Each test's `definition.json` file should be under revision control.

```bash
.
├── test-definitions
|   ├── your-test-name
|       ├── definition.json
```


## JSON Schema
| JSON Property | Description |
| ------------- | ----------- |
| `testType` | The [Identifier]({{ site.baseurl }}/docs/terminology/#identifier) to use for this test |
| `version` | the version for this test |
| `salt` | A salt used in the hashing function used to map String identifiers to integer values. A good convention is to use the test name. Salts that start with "&" can allow you to align bucket assignments by using identical salts. |
| `description` | description of the tests and the features impacted by this test |
| `rule` | (optional) [eligibility rule]({{ site.baseurl }}/docs/test-rules/) for this test |
| `constants` | (optional) collection of variables available in the `rules` for this test |
| `buckets` | An array of `buckets` for this test |
| `allocations` | An array of `allocations` for this test|

## Bucket schema
| JSON Property | Description |
| ------------- | ----------- |
| `name` | human-readable name for this bucket. By convention, this is the same as the name in the application's specification. |
| `value` | int value for this bucket. This bucket must map to a bucket value of an application's specification |
| `description` | human-readable description for the behavior this bucket defines |
| `payload.{payloadType}` | (optional) payload data. The `payloadType` must be consistent with the [payload.type]({{ site.baseurl }}/docs/specification/#payloads) defined in the application's specification |

## Allocation schema
| JSON Property | Description |
| ------------- | ----------- |
| `rule` | (optional) [rule]({{ site.baseurl }}/docs/test-rules/) for this allocation |
| `ranges` | An array of `bucketValue`, `length` pairs describing the bucket distribution |
| `ranges[i].bucketValue` | The bucket value for this part of the distribution |
| `ranges[i].length` | The group size, `[0, 1.0]`, for the given bucket value |



### Example: single allocation

| Property | Value |
| -------- | ----- |
| `testType` | USER |
| `rule` | `null` |
| `buckets` | `inactive, altcolor1, altcolor2, altcolor3, altcolor4`
| `constants` | `{}` |
| `allocations` | 1 allocation |

<table>
  <thead>
    <tr>
      <th>rule</th>
      <th>allocation</th>
    </tr>
  </thead>
  <tbody>
    <tr>
      <td style="width:10%;"><code>Default (<code>rule = null</code>)</code></td>
      <td>
      
        {% include range_allocation_table.html buckets='altcolor1 inactive altcolor2' values="0 -1 1" ranges='25 50 25' %}
      
      </td>
    </tr>
  </tbody>
</table>

{% gist parker/3bb0e94b9b238b48429f 1-definition.json %}


### Example: multiple allocations with rules

| Property | Value |
| -------- | ----- |
| `testType` | USER |
| `buckets` | `inactive, altcolor1, altcolor2, altcolor3, altcolor4`
| `rule` | `${proctor:contains(COUNTRIES, country)}` |
| `constants` | `"COUNTRIES" : ["US", "CA"]` |
| `allocations` | 3 allocations |

<table>
  <thead>
    <tr>
      <th>rule</th>
      <th>allocation</th>
    </tr>
  </thead>
  <tbody>
    <tr>
      <td style="width:10%;"><code>${ua.android && ua.version > 4}</code></td>
      <td>
        {% include range_allocation_table.html buckets='altcolor1 altcolor2' values="0 1" ranges='50 50' %}
      </td>
    </tr>
    <tr>
      <td><code>${ua.IPhone && ua.version > 7}</code></td>
      <td>
        {% include range_allocation_table.html buckets='altcolor3 altcolor4' values="2 3" ranges='50 50' %}
      </td>
    </tr>
    <tr>
      <td><code>Default (<code>rule = null</code>)</code></td>
      <td>      
        {% include range_allocation_table.html buckets='altcolor1 altcolor2 altcolor3 altcolor4' values="0 1 2 3" ranges='25 25 25 25' %}
      </td>
    </tr>
  </tbody>
</table>

{% gist parker/3bb0e94b9b238b48429f 2-definition.json %}

