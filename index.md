---
layout: default
title: Proctor
exclude_toc: true
---
<img src="{{ site.baseurl }}/images/Proctor.png" style="max-width:200px;max-height:200px;clear:both;padding:10px;" />

Proctor is a A/B testing framework written in Java that enables [data-driven product design](http://engineering.indeed.com/blog/2013/05/indeedeng-data-driven-product-design-slides-video/) at Indeed. Proctor consists of data-model, client specification, matrix builder, and java and javascript code generators.

The following Indeed Engineering blog posts describe Proctor in more detail:  <br><br>
<table>
<tr>
    <th>Engineering blog post</th>
    <th>Description</th>
  </tr>
  <tr>
    <td><a href ="http://engineering.indeed.com/blog/2014/06/proctor-a-b-testing-framework/">Proctor: Indeed's A/B Testing Framework</a></td>
    <td>Provides an overview of Proctor and A/B testing at Indeed. <em>This is the first part in a blog series on Proctor at Indeed.</em></td>
    </tr>
   <tr>
    <td><a href="http://engineering.indeed.com/blog/2014/11/how-indeed-uses-proctor-for-a-b-testing/">How Indeed Uses Proctor for A/B Testing</a></td>
    <td>Describes in detail how Indeed integrates Proctor into its development process. <em>This is the second part in a blog series on Proctor at Indeed.</em></td>
    
  </tr>
  <tr>
    <td><a href="http://engineering.indeed.com/blog/2014/09/proctor-pipet-ab-testing-service/">Using Proctor for A/B Testing from a Non-Java Platform</a></td>

<td>Announces the open sourcing of proctor-pipet -- a Java web application that exposes Proctor as a simple REST API accessible over HTTP -- which allows you to do A/B testing in applications written in non-JVM languages. </td>
</tr>
  </table> 
 
GitHub source for Proctor: <a href="https://github.com/indeedeng/proctor">https://github.com/indeedeng/proctor</a><br><br>


## Features
- Consistent tests across multiple applications, services and tools
- Group adjustments without code deploys
- Rule-based group assignment for segmenting users:
  - Logged-in users: 50% A, 50% B
  - Logged-out users: 25% A, 25% B, 25% C, 25% D
- Human-readable test format
- Overriding test groups during internal testing
- Java and JavaScript code generation for A/B tests groups<br><br>


## Getting Started
Read the [Quick Start]({{ site.baseurl }}/docs/quick-start) guide to start running A/B test using Proctor.<br><br>


## Example
[https://github.com/indeedeng/proctor-demo](https://github.com/indeedeng/proctor-demo)


## Discussion
Ask and answer questions in our Q&A forum for Proctor: [indeedeng-proctor-users](https://groups.google.com/forum/#!categories/indeedeng-proctor-users)<br><br>


## License

[Apache License Version 2.0](https://github.com/indeedeng/proctor/blob/master/LICENSE)