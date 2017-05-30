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
  var testContainer = goog.dom.getElement("test-container");

  this.pagerControllers = goog.dom.getElementsByClass("pager-controller");
  this.testsPerPage = +testContainer.getAttribute("data-tests-per-page");
  this.currentPage = +testContainer.getAttribute("data-page");
  this.models = this.createModels(matrix, testContainer);

  this.registerControllers(this.pagerControllers);
  new indeed.proctor.filter.Filter(this.models, filterContainer, goog.bind(this.updatePager, this));
  new indeed.proctor.filter.Sorter(this.models, filterContainer, testContainer, goog.bind(this.updatePager, this));
  this.matchedTestLength = this.models.length;
};

indeed.proctor.filter.Pager.prototype.registerControllers = function (pagerControllers) {
  goog.array.forEach(pagerControllers, function (controller) {
    var prevButton = controller.querySelector(".pager-prev");
    var nextButton = controller.querySelector(".pager-next");
    goog.events.listen(prevButton, goog.events.EventType.CLICK, goog.bind(function () {
      this.updatePager(this.currentPage - 1);
    }, this));
    goog.events.listen(nextButton, goog.events.EventType.CLICK, goog.bind(function () {
      this.updatePager(this.currentPage + 1);
    }, this));
  }, this);
};

indeed.proctor.filter.Pager.prototype.updateControllers = function (pagerControllers) {
  goog.array.forEach(pagerControllers, function (controller) {
    var currentPageText = controller.querySelector(".pager-current-page");
    var pageNumText = controller.querySelector(".pager-page-num");
    goog.dom.setTextContent(currentPageText, this.currentPage + 1);
    goog.dom.setTextContent(pageNumText, Math.ceil(this.matchedTestLength / this.testsPerPage));
  }, this);
};

/**
 * This refreshes the showing tests and pager controllers according to the given page.
 */
indeed.proctor.filter.Pager.prototype.updatePager = function (page) {
  var oldPage = this.currentPage;
  if (page != null) {
    this.currentPage = Math.max(0, Math.min(page, Math.ceil(this.matchedTestLength / this.testsPerPage) - 1));
  }
  var matched = 0;
  goog.array.forEach(this.models, function (model) {
    model.dom.style.display = "none";
    if (!model.excluded) {
      if (this.insideOfThePageRange(matched, this.currentPage, this.testsPerPage)) {
        model.dom.style.display = "";
      }
      matched++;
    }
  }, this);
  this.matchedTestLength = matched;

  this.updateControllers(this.pagerControllers);
  if (oldPage != this.currentPage) {
    this.scrollToTop();
  }
};

indeed.proctor.filter.Pager.prototype.insideOfThePageRange = function(i, page, testsPerPage) {
  return testsPerPage * page <= i && i < testsPerPage * (page + 1);
};

indeed.proctor.filter.Pager.prototype.scrollToTop = function() {
  window.scrollTo(window.pageXOffset, 0);
};

indeed.proctor.filter.Pager.prototype.createModels = function (matrix, testContainer) {
  return goog.array.map(testContainer.querySelectorAll('.ui-test-definition'), function (testDefinitionNode) {
    var testName = goog.dom.getTextContent(testDefinitionNode.querySelector(".mtn"));
    var definition = matrix.tests[testName];
    return {
      testName: testName,
      definition: definition,
      dom: testDefinitionNode.parentNode,
      excluded: false // used for Filter
    };
  });
};