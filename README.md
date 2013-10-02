# Proctor
Proctor is a A/B testing framework written in Java that enables [data-driven product design](http://engineering.indeed.com/blog/2013/05/indeedeng-data-driven-product-design-slides-video/) at Indeed. 

Proctor consists of data-model, client specification, client loader, matrix builder, java code generator.

# Features:
- consistent tests across multiple applications
- group assignment adjustments without code deploys
- rule-based group assignment: e.g. US users experience 50% A, 50% B and non-US users are 25% A, 25% B, 25% C, 25% D
- human-readable test format
- forcing of test groups for internal testing
- java code generation for A/B tests groups

# Installation
See [Quick Start](http://indeedeng.github.io/proctor/docs/quick-start) guide

# Example
See [proctor demo](http://www.github.com/indeedeng/proctor-demo)

# Documentation
http://indeedeng.github.io/proctor

## Building documentation locally

```bash
$ git checkout gh-pages
$ gem install bundler
$ bundle install
$ rake clean serve
  => open http://localhost:4000/ in browser
```

# Discussion

Join the [indeedeng-proctor-users](https://groups.google.com/d/forum/indeedeng-proctor-users) mailing list to ask questions and discuss use of Proctor.

# Deploying

# Contributing

# See Also (other A/B test frameworks)
- [seatgeek/sixpack](https://github.com/seatgeek/sixpack)
- [bitlove/rollout](https://github.com/bitlove/rollout)
- [maccman/abba](https://github.com/maccman/abba)
- [assaf/vanity](https://github.com/assaf/vanity)

# License

[Apache License Version 2.0](https://github.com/indeedeng/proctor/blob/master/LICENSE)
