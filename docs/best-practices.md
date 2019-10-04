---
layout: default
title: Best Practices
permalink: /docs/best-practices/
---

## Gotchas
### "Inactive" vs "not allocated"
There is a difference in how Proctor handles "not allocated" (failed to match rule) and being put into an "inactive" group.
### Invalid matrix a given test
Single test will be invalid and will be "random" 100% -1 bucket
add interface to log and capture the load. ("loadListener")
### Preventing user drift using group changes
When increasing test sizes, ensure that users are not unintentionally switched from the `test` to `control` buckets.

Use one of the following approaches to prevent user drift:

* Assign groups starting at the "ends" of your allocation ranges and increase the allocations:

  {% include range_allocation_table.html buckets='control inactive test' values="0 -1 1" ranges='10 80 10' %}
  {% include range_allocation_table.html buckets='control inactive test' values="0 -1 1" ranges='20 60 20' %}

* Assign groups starting at the "beginning" of your allocation ranges and increase allocations by using some `inactive` users:
  {% include range_allocation_table.html buckets='control test inactive' values="0 1 -1" ranges='10 10 80' %}
  {% include range_allocation_table.html buckets='control test control test inactive' values="0 1 0 1 -1" ranges='10 10 10 10 60' %}

 Ensure that users do not drive from test to control during allocation changes.



## Use cases
* Kill switch
* Dark deploy
* Feature toggle
* Reduce deploy dependencies
