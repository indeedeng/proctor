goog.provide("indeed.proctor.filter.Sorter");

goog.require("goog.dom.dataset");
goog.require("goog.format");
goog.require('indeed.proctor.filter.Favorites');

/**
 * Sorter controller for detecting form changes and sorting models
 * @param models parent models that contains test name and definition
 * @param filterContainer container for filter form
 * @param testContainer container for test form
 * @param updateCallback callback called after any sorting
 * @constructor
 */
indeed.proctor.filter.Sorter = function (models, filterContainer, testContainer, updateCallback) {
    this.testContainer = testContainer;
    this.favorites = new indeed.proctor.filter.Favorites(testContainer);
    this.sortedByNode = filterContainer.querySelector(".js-filter-sorted-by");
    this.models = models;

    this.addModels(this.models);

    goog.array.forEach(this.options, goog.bind(function(x, index){
        goog.dom.appendChild(this.sortedByNode,
            goog.dom.createDom(goog.dom.TagName.OPTION, {"value": index}, x.name));
    }, this));

    this.updateCallback = updateCallback;
    goog.events.listen(this.sortedByNode, goog.events.EventType.CHANGE, goog.bind(function(){
        this.refreshOrder();
        this.updateCallback();
    }, this));

    this.sortWithDefaultOrder();
};

indeed.proctor.filter.Sorter.prototype.sortWithDefaultOrder = function() {
    this.refreshOrder();
};

indeed.proctor.filter.Sorter.prototype.options = [
    {
        name: "favorites first",
        keyFunction: function(x){
            var sortKey = (''+(999999999 - x.relevancyRank)).concat(x.testName);
            return sortKey;
        },
        comparator: goog.array.defaultCompare
    },
    {
        name: "test name",
        keyFunction: function(x){ return x.testName; },
        comparator: goog.array.defaultCompare
    },
    {
        name: "updated date",
        keyFunction: function(x){ return x.updated; },
        comparator: goog.array.inverseDefaultCompare
    }
];

indeed.proctor.filter.Sorter.prototype.refreshOrder = function () {
    var index = this.sortedByNode.value;
    var option = this.options[index];
    goog.array.sortByKey(
        this.models,
        option.keyFunction,
        option.comparator
    );
    var testContainer = this.testContainer;
    goog.array.forEach(this.models, function(model) {
        goog.dom.appendChild(testContainer, model.dom);
    });
};

indeed.proctor.filter.Sorter.prototype.addModels = function (models) {
    this.favorites.refresh();
    models.forEach(function(model){
        model.updated = goog.dom.dataset.get(model.dom, "updated");
        model.relevancyRank = this.favorites.rankOf(model.testName);
    }, this);
};
