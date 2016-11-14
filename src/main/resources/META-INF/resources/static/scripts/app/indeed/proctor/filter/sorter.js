goog.provide("indeed.proctor.filter.Sorter");

goog.require("goog.dom.dataset");

indeed.proctor.filter.Sorter = function (filterContainer, testContainer) {
    var sorter = this;
    this.testContainer = testContainer;
    this.models = this.createModels(testContainer);
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
    }
];

indeed.proctor.filter.Sorter.prototype.refreshOrder = function () {
    var index = this.sortedByNode.value;
    var option = this.options[index];
    this.sortModels(option);

    var testContainer = this.testContainer;
    goog.array.forEach(this.models, function(x) {
        goog.dom.appendChild(testContainer, x.dom);
    });
}

indeed.proctor.filter.Sorter.prototype.sortModels = function (option) {
    goog.array.sortByKey(
        this.models,
        option.keyFunction,
        option.comparator
    );
}

indeed.proctor.filter.Sorter.prototype.createModels = function (testContainer) {
    var children = goog.dom.getChildren(testContainer);
    var models = goog.array.map(children, function(child){
        var updated = goog.dom.dataset.get(child, "updated");
        var testName = goog.dom.getTextContent(child.querySelector(".mtn"));
        return {
            dom: child,
            updated: updated,
            name: testName
        };
    });
    return models;
}
