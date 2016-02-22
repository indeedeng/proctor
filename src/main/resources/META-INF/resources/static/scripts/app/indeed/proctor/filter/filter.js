goog.provide('indeed.proctor.filter');

indeed.proctor.filter.Filter = function (matrix, container) {
    this.textNode = container.querySelector(".js-filter-text");
    this.filterTypeNode = container.querySelector(".js-filter-type");
    this.filterActiveNode = container.querySelector(".js-filter-active");
    this.numMatchedNode = container.querySelector(".js-num-matched");
    this.numAllNode = container.querySelector(".js-num-all");
    this.models = this.createModels(matrix);
    goog.events.listen(this.filterTypeNode, "change", this.refreshFilter.bind(this));
    goog.events.listen(this.filterActiveNode, "change", this.refreshFilter.bind(this));

    this.textNode.focus();

    var timer;
    goog.events.listen(this.textNode, "input", function (e) {
        if (timer) {
            clearTimeout(timer);
        }
        setTimeout(this.refreshFilter.bind(this), 400);
    }.bind(this));
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

    this.models.forEach(function (model) {
        var matched = texts.every(function (text) {
            return model.texts[key].indexOf(text) >= 0;
        });
        if (matched) {
            if (active == "active") {
                matched = model.definition.allocations.some(function (allocation) {
                    return allocation.ranges.every(function (range) {
                        return range.length < 1;
                    });
                });
            } else if (active == "inactive") {
                matched = model.definition.allocations.every(function (allocation) {
                    return allocation.ranges.some(function (range) {
                        return range.length == 1;
                    });
                });
            }
        }
        if (matched) {
            numMatched++;
            model.dom.style.display = "";
        } else {
            model.dom.style.display = "none";
        }
    });
    this.numMatchedNode.textContent = numMatched;
};

indeed.proctor.filter.Filter.prototype.createModels = function (matrix) {
    var models = [];
    var divs = document.querySelectorAll("div.ui-test-definition");
    for (var i = 0; i < divs.length; i++) {
        var div = divs[i];
        var testName = div.querySelector(".mtn").textContent;
        var definition = matrix.tests[testName];
        var model = {
            definition: definition,
            dom: div.parentNode,
            texts: {
                testName: normalize(testName),
                description: normalize(definition.description || ""),
                rule: normalize((definition.rule || "") + definition.allocations.map(function (allocation) {
                        return allocation.rule || "";
                    }).join(" ")),
                bucket: normalize(definition.buckets.map(function (bucket) {
                    return bucket.name;
                }).join(" ")),
                bucketDescription: normalize(definition.buckets.map(function (bucket) {
                    return bucket.description;
                }).join(" ")),
                testType: normalize(definition.testType),
                salt: normalize(definition.salt)
            }
        };
        var all = [];
        for (var j in model.texts) {
            all.push(model.texts[j]);
        }
        model.texts.all = all.join(" ");
        models.push(model);
    }
    this.numMatchedNode.textContent = this.numAllNode.textContent = models.length;
    return models;

    function normalize(text) {
        return text.toLowerCase().replace(/\s+/g, " ");
    }
};

