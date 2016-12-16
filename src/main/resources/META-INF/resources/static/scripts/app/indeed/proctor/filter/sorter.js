goog.provide("indeed.proctor.filter.Sorter");

goog.require("goog.dom.dataset");
goog.require("goog.format");

indeed.proctor.filter.Sorter = function (filterContainer, testContainer, favorites) {
    var sorter = this;
    this.testContainer = testContainer;
    this.favorites = favorites;
    this.sortedByNode = filterContainer.querySelector(".js-filter-sorted-by");

    goog.array.forEach(this.options, function(x, index){
        goog.dom.appendChild(sorter.sortedByNode,
            goog.dom.createDom(goog.dom.TagName.OPTION, {"value": index}, x.name));
    });

    goog.events.listen(this.sortedByNode, goog.events.EventType.CHANGE, function(){
        sorter.refreshOrder();
    });
}

indeed.proctor.filter.Sorter.prototype.options = [
    {
        name: "test name",
        keyFunction: function(x){ return x.name; },
        comparator: goog.array.defaultCompare
    },
    {
        name: "updated date",
        keyFunction: function(x){ return x.updated; },
        comparator: goog.array.inverseDefaultCompare
    },
    {
        name: "favorites first",
        keyFunction: function(x){
            var sortKey = (''+(999999999 - x.relevancyRank)).concat(x.name);
            return sortKey;
        },
        comparator: goog.array.defaultCompare
    }
];

indeed.proctor.filter.Sorter.prototype.refreshOrder = function () {
    var index = this.sortedByNode.value;
    var option = this.options[index];
    var models = this.createModels();
    goog.array.sortByKey(
        models,
        option.keyFunction,
        option.comparator
    );
    var testContainer = this.testContainer;
    goog.array.forEach(models, function(x) {
        goog.dom.appendChild(testContainer, x.dom);
    });
}



indeed.proctor.filter.Sorter.prototype.createModels = function () {
    var children = goog.dom.getChildren(this.testContainer);
    var favorites = this.favorites;
    var models = goog.array.map(children, function(child){
        var updated = goog.dom.dataset.get(child, "updated");
        var testName = goog.dom.getTextContent(child.querySelector(".mtn"));
        var relevancyRank = favorites.rankOf(testName);
        return {
            dom: child,
            updated: updated,
            name: testName,
            relevancyRank: relevancyRank
        };
    });
    return models;
}
