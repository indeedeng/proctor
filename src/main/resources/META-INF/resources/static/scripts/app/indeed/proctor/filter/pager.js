goog.provide("indeed.proctor.filter.Pager");

goog.require('indeed.proctor.filter.Filter');
goog.require('indeed.proctor.filter.Sorter');

/**
 * Pager controller for handling which tests to show
 * @param matrix testMatrixDefinition object
 * @constructor
 */
indeed.proctor.filter.Pager = function (matrix) {
  var filterContainer = goog.dom.getElement("filter-container");
  var testContainer = goog.dom.getElement("test-container");

  this.numMatchedNode = filterContainer.querySelector(".js-filter-num-matched");
  this.pagerControllers = goog.dom.getElementsByClass("pager-controller");
  this.testsPerPage = Number(testContainer.getAttribute("data-tests-per-page"));
  this.models = this.createModels(matrix, testContainer);

  new indeed.proctor.filter.Filter(this.models, filterContainer, goog.bind(this.resetPage, this));
  new indeed.proctor.filter.Sorter(this.models, filterContainer, testContainer, goog.bind(this.resetPage, this));

  this.currentPage = this.getPageFromFragment(0);
  this.registerControllers(this.pagerControllers);
  this.updatePager();
};

indeed.proctor.filter.Pager.PAGE_FRAGMENT_KEY = "page";

indeed.proctor.filter.Pager.prototype.registerControllers = function (pagerControllers) {
  goog.array.forEach(pagerControllers, function (controller) {
    var prevButton = controller.querySelector(".pager-prev");
    var nextButton = controller.querySelector(".pager-next");
    goog.events.listen(prevButton, goog.events.EventType.CLICK, goog.bind(function () {
      this.setPage(this.currentPage - 1);
    }, this));
    goog.events.listen(nextButton, goog.events.EventType.CLICK, goog.bind(function () {
      this.setPage(this.currentPage + 1);
    }, this));
  }, this);

  goog.events.listen(window, goog.events.EventType.HASHCHANGE, goog.bind(function () {
    this.updatePager();
  }, this));
};

indeed.proctor.filter.Pager.prototype.updateControllers = function (pagerControllers) {
  goog.array.forEach(pagerControllers, function (controller) {
    var currentPageText = controller.querySelector(".pager-current-page");
    var pageNumText = controller.querySelector(".pager-page-num");
    goog.dom.setTextContent(currentPageText, this.currentPage + 1);
    goog.dom.setTextContent(pageNumText, Math.ceil(this.getNumMatchedTest() / this.testsPerPage));
  }, this);
};

/**
 * This refreshes the showing tests and pager controllers according to the given page.
 */
indeed.proctor.filter.Pager.prototype.updatePager = function () {
  var oldPage = this.currentPage;
  this.currentPage = this.adjustPage(this.getPageFromFragment(this.currentPage));
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

  this.updateControllers(this.pagerControllers);
  if (oldPage != this.currentPage) {
    this.scrollToTop();
  }
};

/**
 * return how many number of tests are enabled by filter.
 * @returns {number}
 */
indeed.proctor.filter.Pager.prototype.getNumMatchedTest = function () {
  return Number(goog.dom.getTextContent(this.numMatchedNode));
};

indeed.proctor.filter.Pager.prototype.resetPage = function () {
  this.setPage(0);
  this.updatePager();
};

/**
 * check that given index is inside the range of given page
 * @param i index of the test
 * @param page current page number
 * @param testsPerPage
 * @returns {boolean}
 */
indeed.proctor.filter.Pager.prototype.insideOfThePageRange = function (i, page, testsPerPage) {
  return testsPerPage * page <= i && i < testsPerPage * (page + 1);
};

indeed.proctor.filter.Pager.prototype.scrollToTop = function () {
  window.scrollTo(window.pageXOffset, 0);
};

indeed.proctor.filter.Pager.prototype.getPageFromFragment = function (defaultValue) {
  var hash = window.location.hash;
  if (hash && hash[0] == '#') {
    hash = hash.substring(1);
  }
  return this.adjustPage((hash && !isNaN(hash)) ? Number(hash) - 1 : defaultValue);
};

indeed.proctor.filter.Pager.prototype.setPage = function (page) {
  window.location.hash = String(this.adjustPage(page) + 1);
};

/**
 * adjust the page number to fit the proper range
 * @param page
 * @returns {number}
 */
indeed.proctor.filter.Pager.prototype.adjustPage = function (page) {
  return Math.max(0, Math.min(page, Math.ceil(this.getNumMatchedTest() / this.testsPerPage) - 1));
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