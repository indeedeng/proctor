---
layout: default
title: Proctor
exclude_toc: true
---

Proctor is a A/B testing framework written in Java that enables [data-driven product design](http://engineering.indeed.com/blog/2013/05/indeedeng-data-driven-product-design-slides-video/) at Indeed. 

Proctor consists of data-model, client specification, matrix builder, java code generator.

Source: https://github.com/indeedeng/proctor

# Features:
- consistent tests across multiple applications, services and tools
- group adjustments without code deploys
- rule-based group assignment for segmenting users:
  - logged-in users: 50% A, 50% B 
  - logged-out users: 25% A, 25% B, 25% C, 25% D
- human-readable test format
- overriding test groups during internal testing
- java code generation for A/B tests groups

# Getting Started
See the [Quick Start]({{ site.baseurl }}/docs/quick-start) guide to start running A/B test using Proctor

# Example
- https://github.com/indeedeng/proctor-demo

# License

[Apache License Version 2.0](https://github.com/indeedeng/proctor/blob/master/LICENSE)
