goog.provide('indeed.proctor.filter.Filter');

goog.require('goog.async.Delay');

/**
 * Filter controller for detecting form changes and filtering DOM
 * @param models parent models that contains test name and definition
 * @param container container for filter form
 * @param refreshCallback callback for when refreshing needed
 * @constructor
 */
indeed.proctor.filter.Filter = function (models, container, refreshCallback) {
    this.textNode = container.querySelector(".js-filter-text");
    this.filterTypeNode = container.querySelector(".js-filter-type");
    this.filterActiveNode = container.querySelector(".js-filter-active");
    this.numMatchedNode = container.querySelector(".js-filter-num-matched");
    this.numAllNode = container.querySelector(".js-filter-num-all");
    this.models = models;

    this.addModels(this.models);

    goog.dom.setTextContent(this.numMatchedNode, this.models.length);
    goog.dom.setTextContent(this.numAllNode, this.models.length);
    this.textNode.focus();

    this.refreshCallback = refreshCallback;
    var delay = new goog.async.Delay(goog.bind(function() {
      this.refreshFilter();
      this.refreshCallback(0);
    }, this));
    goog.events.listen(this.textNode, goog.events.EventType.INPUT, function(){
        delay.start(400);
    });
    goog.events.listen(this.filterTypeNode, goog.events.EventType.CHANGE, function(){
        delay.start(100);
    });
    goog.events.listen(this.filterActiveNode, goog.events.EventType.CHANGE, function(){
        delay.start(100);
    });
};
indeed.proctor.filter.Filter.prototype.refreshFilter = function () {
    var radios = this.filterActiveNode.querySelectorAll("input");
    var active = "all";
    for (var i = 0; i < radios.length; i++) {
        var radio = radios[i];
        if (radio.checked) {
            active = radio.value;
        }
    }
    this.filter(this.textNode.value, this.filterTypeNode.value, active);
};
indeed.proctor.filter.Filter.prototype.filter = function (text, key, active) {
    var texts = text.toLowerCase().split(" ");
    var numMatched = 0;

    goog.array.forEach(this.models, function (model) {
        var matched = goog.array.every(texts, function (text) {
            return model.texts[key].indexOf(text) >= 0;
        });
        if (matched) {
            if (active == "active") {
                matched = goog.array.some(model.definition.allocations, function (allocation) {
                    return goog.array.every(allocation.ranges, function (range) {
                        return range.length < 1;
                    });
                });
            } else if (active == "inactive") {
                matched = goog.array.every(model.definition.allocations, function (allocation) {
                    return goog.array.some(allocation.ranges, function (range) {
                        return range.length == 1;
                    });
                });
            }
        }
        model.excluded = !matched;
        if (matched) {
            numMatched++;
            //model.dom.style.display = "";
        } else {
            //model.dom.style.display = "none";
        }
    });
    goog.dom.setTextContent(this.numMatchedNode, numMatched);
};

indeed.proctor.filter.Filter.prototype.addModels = function (models) {
    models.forEach(function(model){
      model.texts = this.getTexts(model.testName, model.definition);
    }, this);
};


indeed.proctor.filter.Filter.prototype.getTexts = function (testName, definition) {
    texts = {
      testName: normalize(testName),
      description: normalize(definition.description || ""),
      rule: normalize((definition.rule || "") + goog.array.map(definition.allocations, function (allocation) {
          return allocation.rule || "";
        }).join(" ")),
      bucket: normalize(goog.array.map(definition.buckets, function (bucket) {
        return bucket.name;
      }).join(" ")),
      bucketDescription: normalize(goog.array.map(definition.buckets, function (bucket) {
        return bucket.description;
      }).join(" ")),
      testType: normalize(definition.testType),
      salt: normalize(definition.salt)
    };
    texts.all = goog.object.getValues(texts).join(" ");
    return texts;

    function normalize(text) {
        return text.toLowerCase().replace(/\s+/g, " ");
    }
};