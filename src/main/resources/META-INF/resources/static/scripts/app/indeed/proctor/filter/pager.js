goog.provide("indeed.proctor.filter.Pager");

goog.require('indeed.proctor.filter.Filter');
goog.require('indeed.proctor.filter.Sorter');
goog.require('indeed.proctor.filter.Favorites');

/**
 * Pager controller for handling which tests to show
 * @param matrix testMatrixDefinition object
 * @constructor
 */
indeed.proctor.filter.Pager = function (matrix) {
  var filterContainer = goog.dom.getElement("filter-container");
  var pagerContainer = goog.dom.getElement("pager-container");
  var testContainer = goog.dom.getElement("test-container");

  this.testsPerPage = +pagerContainer.getAttribute("data-tests-per-page");
  this.currentPage = +pagerContainer.getAttribute("data-page");
  this.prevButton = pagerContainer.querySelector(".pager-prev");
  this.nextButton = pagerContainer.querySelector(".pager-next");
  this.currentPageText = pagerContainer.querySelector(".pager-current-page");
  this.pageNumText = pagerContainer.querySelector(".pager-page-num");

  this.models = this.createModels(matrix, testContainer);
  new indeed.proctor.filter.Filter(this.models, filterContainer, goog.bind(this.refreshPager, this));
  new indeed.proctor.filter.Sorter(this.models, filterContainer, testContainer, goog.bind(this.refreshPager, this));
  this.matchedTestLength = this.models.length;

  goog.events.listen(this.prevButton, goog.events.EventType.CLICK, goog.bind(function () {
    this.refreshPager(this.currentPage - 1);
  }, this));
  goog.events.listen(this.nextButton, goog.events.EventType.CLICK, goog.bind(function () {
    this.refreshPager(this.currentPage + 1);
  }, this));
};

indeed.proctor.filter.Pager.prototype.refreshPager = function (page) {
  if (page != null) {
    var oldPage = this.currentPage;
    this.currentPage = Math.max(0, Math.min(page, Math.ceil(this.matchedTestLength / this.testsPerPage) - 1));
  }
  var matched = 0;
  goog.array.forEach(this.models, function (model) {
    model.dom.style.display = "none";
    if (!model.excluded) {
      if (this.testsPerPage * this.currentPage <= matched && matched < this.testsPerPage * (this.currentPage + 1)) {
        model.dom.style.display = "";
      }
      matched++;
    }
  }, this);
  this.matchedTestLength = matched;
  goog.dom.setTextContent(this.currentPageText, this.currentPage + 1);
  goog.dom.setTextContent(this.pageNumText, Math.ceil(this.matchedTestLength / this.testsPerPage));

  if (oldPage != this.currentPage) {
    window.scrollTo(0, 0);
  }
};

indeed.proctor.filter.Pager.prototype.createModels = function (matrix, testContainer) {
  return goog.array.map(testContainer.querySelectorAll('.ui-test-definition'), function (testDefinitionNode) {
    var testName = goog.dom.getTextContent(testDefinitionNode.querySelector(".mtn"));
    var definition = matrix.tests[testName];
    return {
      testName: testName,
      definition: definition,
      dom: testDefinitionNode.parentNode,
      updated: goog.dom.dataset.get(testDefinitionNode, "updated"),
      excluded: false // used for Filter
    };
  });
};