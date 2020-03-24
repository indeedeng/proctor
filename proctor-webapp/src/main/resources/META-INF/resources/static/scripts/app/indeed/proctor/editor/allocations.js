goog.provide('indeed.proctor.editor.AllocationsEditor');

goog.require('goog.dom');
goog.require('goog.events');
goog.require('goog.events.EventHandler');
goog.require('goog.events.EventTarget');
goog.require('goog.string');
goog.require('indeed.foundation.forms');
goog.require('indeed.proctor.forms');



/**
 *
 * @param {Element} container Root Element.
 * @param {Object} definition Definition JSON.
 * @param {boolean} isCreate Is new test.
 * @constructor
 * @extends {goog.events.EventTarget}
 */
indeed.proctor.editor.AllocationsEditor =
    function(container, definition, isCreate) {
  indeed.proctor.editor.AllocationEditor.base(this, 'constructor');

  /** @type {Element} */
  this.container = container;

  /** @type {goog.dom.DomHelper} @private */
  this.dom_ = goog.dom.getDomHelper(container);

  /** @type {Array} */
  this.buckets = goog.array.clone(definition['buckets']);

  /** @type {Array} */
  this.allocations = definition['allocations'];

  /** @type {Array.<indeed.proctor.editor.AllocationEditor>} @private */
  this.allocationEditors_ = [];

  /** @type {goog.events.EventHandler} @private */
  this.handler_ = new goog.events.EventHandler(this);

  this.bind_();
};
goog.inherits(indeed.proctor.editor.AllocationsEditor, goog.events.EventTarget);


/**
 * Identify elements within container + add event listeners
 * @private
 */
indeed.proctor.editor.AllocationsEditor.prototype.bind_ = function() {
  var editors = this.dom_.getElementsByTagNameAndClass(null,
      'js-allocation-editor', this.container);
  for (var i = 0, len = editors.length; i < len; i++) {
    // assert i < this.allocations.length

    var allocationEditor = new indeed.proctor.editor.AllocationEditor(editors[i], i,
        this.buckets, this.allocations[i], i == editors.length - 1, this);
    this.handler_.listen(allocationEditor, 'deleteAllocation', this.onDeleteAllocationClick_);
    this.handler_.listen(allocationEditor, 'addAllocation', this.onAddAllocationClick_);

    this.allocationEditors_.push(allocationEditor);
  }
};


/**
 *
 * @param {Array} buckets Buckets.
 */
indeed.proctor.editor.AllocationsEditor.prototype.bucketsUpdated =
    function(buckets) {
  this.buckets = goog.array.clone(buckets);
  for (var i = 0; i < this.allocationEditors_.length; i++) {
    this.allocationEditors_[i].bucketsUpdated(buckets);
  }
};


/**
 * @return {Object} JSON representation.
 */
indeed.proctor.editor.AllocationsEditor.prototype.toJSON = function() {
  var els = goog.array.concat(
      goog.array.toArray(this.dom_.getElementsByTagNameAndClass(
          goog.dom.TagName.INPUT, 'json', this.container)),
      goog.array.toArray(this.dom_.getElementsByTagNameAndClass(
          goog.dom.TagName.SELECT, 'json', this.container))
      );
  var definition = indeed.proctor.forms.toJSON(els);
  return definition['allocation'];
};


/**
 * @return {boolean} Flag indicating if this widget is valid.
 */
indeed.proctor.editor.AllocationsEditor.prototype.validate = function() {
  var isValid = this.allocationEditors_.length > 0;
  for (var i = 0; i < this.allocationEditors_.length; i++) {
    isValid = this.allocationEditors_[i].validate() && isValid;
  }
  return isValid;
};

/**
 * @return {boolean} Flag indicating if this widget is active.
 */
indeed.proctor.editor.AllocationsEditor.prototype.checkActive = function () {
  return this.allocationEditors_.some(function (allocationEditor) {
    return allocationEditor.checkActive();
  });
};

/**
 * @param {goog.events.Event} e delete allocation event with an AllocationEditor as target.
 * @private
 */
indeed.proctor.editor.AllocationsEditor.prototype.onDeleteAllocationClick_ = function(e) {
  var referenceAllocationEditor = e.target;
  this.handler_.unlisten(referenceAllocationEditor, ['deleteAllocation', 'addAllocation']);

  this.container.removeChild(referenceAllocationEditor.container);
  goog.array.removeAt(this.allocationEditors_, referenceAllocationEditor.index);
  referenceAllocationEditor.dispose();
  this.updateAllocationEditorIndices_();
};


/**
 * @param {goog.events.Event} e add allocation event with an AllocationEditor as target.
 * @private
 */
indeed.proctor.editor.AllocationsEditor.prototype.onAddAllocationClick_ = function(e) {
  var referenceAllocationEditor = e.target;
  var allocationEditor = this.makeEmptyAllocationEditor_(referenceAllocationEditor);
  this.handler_.listen(allocationEditor, 'deleteAllocation', this.onDeleteAllocationClick_);
  this.handler_.listen(allocationEditor, 'addAllocation', this.onAddAllocationClick_);

  this.container.insertBefore(allocationEditor.container, referenceAllocationEditor.container);
  goog.array.insertAt(this.allocationEditors_, allocationEditor, referenceAllocationEditor.index);
  this.updateAllocationEditorIndices_();
};


/**
 *
 * @param {indeed.proctor.editor.AllocationEditor} referenceAllocationEditor used as a reference to create a new one.
 * @return {indeed.proctor.editor.AllocationEditor} a new, non-default AllocationEditor with no allocations.
 * @private
 */
indeed.proctor.editor.AllocationsEditor.prototype.makeEmptyAllocationEditor_ = function(referenceAllocationEditor) {
  var emptyContainer = referenceAllocationEditor.container.cloneNode(true);

  // Show rule and delete allocation button
  goog.dom.classes.remove(goog.dom.getElementByClass('js-rule-container', emptyContainer), 'hide');
  goog.dom.classes.remove(goog.dom.getElementByClass('js-delete-allocation', emptyContainer), 'hide');

  // Clear existing allocations and error messages
  goog.dom.removeChildren(goog.dom.getElementByClass('js-allocations', emptyContainer));
  goog.dom.removeChildren(goog.dom.getElementByClass('allocations-msg-container', emptyContainer));

  // Clear existing allocation bars
  var allocationBars = goog.dom.getElementsByClass('ui-allocation-bar', emptyContainer);
  for (var i = 0; i < allocationBars.length; i++) {
    goog.dom.removeChildren(allocationBars[i]);
  }

  // Clear input fields
  var inputs = goog.dom.getElementsByTagNameAndClass(goog.dom.TagName.INPUT, null, emptyContainer);
  for (var i = 0; i < inputs.length; i++) {
    if (inputs[i].getAttribute('name') == 'add-bucket.length') {
      inputs[i].value = '0.0';
    } else {
      inputs[i].value = '';
    }
    indeed.foundation.forms.removeError(inputs[i]);
  }

  // Clear select fields
  var selects = goog.dom.getElementsByTagNameAndClass(goog.dom.TagName.SELECT, null, emptyContainer);
  for (var i = 0; i < selects.length; i++) {
    selects[i].selectedIndex = 0;
    indeed.foundation.forms.removeError(selects[i]);
  }

  return new indeed.proctor.editor.AllocationEditor(emptyContainer, referenceAllocationEditor.index,
      this.buckets, {'rule': null, 'ranges': []}, false)
};


/**
 * Updates the indices of the allocation editors according to their actual position.
 * @private
 */
indeed.proctor.editor.AllocationsEditor.prototype.updateAllocationEditorIndices_ = function() {
  for (var i = 0; i < this.allocationEditors_.length; i++) {
    var allocationEditor = this.allocationEditors_[i];
    allocationEditor.index = i;

    var allocationContainer = allocationEditor.container;
    var inputs = goog.dom.getElementsByTagNameAndClass(goog.dom.TagName.INPUT, null, allocationContainer);
    var selects = goog.dom.getElementsByTagNameAndClass(goog.dom.TagName.SELECT, null, allocationContainer);
    var elements = goog.array.concat(goog.array.toArray(inputs), goog.array.toArray(selects));

    var nameRegex = new RegExp('allocations\[[0-9]+\]', 'g');
    var replacement = 'allocations[' + allocationEditor.index + ']';

    goog.array.forEach(elements, function(element) {
      var inputName = element.getAttribute('name');
      var newName = inputName.replace(nameRegex, replacement);
      element.setAttribute('name', newName);
    });
  }
};


/**
 *
 * @param {Element} container Root Element.
 * @param {number} index Allocation index.
 * @param {Array} buckets JSON Buckets.
 * @param {Object} allocation JSON allocation.
 * @param {boolean} isDefault Is default allocation.
 * @param {Object} allocationsEditor allocations editor as parent event target
 * @constructor
 * @extends {goog.events.EventTarget}
 */
indeed.proctor.editor.AllocationEditor =
    function(container, index, buckets, allocation, isDefault, allocationsEditor) {
  indeed.proctor.editor.AllocationEditor.base(this, 'constructor');
  /** @type {goog.dom.DomHelper} @private */
  this.dom_ = goog.dom.getDomHelper(container);
  /** @type {Element} */
  this.container = container;
  /** @type {number} */
  this.index = index;
  this.buckets = goog.array.clone(buckets);
  this.allocation = allocation;
  this.ranges = goog.array.clone(allocation['ranges']);
  this.prevRanges = JSON.parse(JSON.stringify(this.ranges));  // Deep copy

  this.isDefault = isDefault;

  /** @type {goog.events.EventHandler} @private */
  this.handler_ = new goog.events.EventHandler(this);
  this.registerDisposable(this.handler_);

  this.bind_();
  this.setParentEventTarget(allocationsEditor);
};
goog.inherits(indeed.proctor.editor.AllocationEditor, goog.events.EventTarget);


/**
 * Identify elements within container + add event listeners
 * @private
 */
indeed.proctor.editor.AllocationEditor.prototype.bind_ = function() {
  this.addRatioRow = this.dom_.getElementByClass('js-add-ratio-row',
                                                 this.container);

  var rows = this.dom_.getElementsByTagNameAndClass(null, 'js-ratio-row',
                                                    this.container);
  for (var i = 0; i < rows.length; i++) {
    this.bindRow_(rows[i], i);
  }
  var addRatio = this.dom_.getElementsByTagNameAndClass(null, 'js-add-ratio',
                                                        this.container);
  for (var i = 0; i < addRatio.length; i++) {
    this.handler_.listen(addRatio[i], goog.events.EventType.CLICK,
                         this.onAddRatioClick_);
  }

  var deleteAllocation = this.dom_.getElementByClass('js-delete-allocation', this.container);
  this.handler_.listen(deleteAllocation, goog.events.EventType.CLICK,
      function(e) {
        e.preventDefault();
        this.dispatchEvent({'type': 'deleteAllocation'});
      });

  var addAllocation = this.dom_.getElementByClass('js-add-allocation', this.container);
  this.handler_.listen(addAllocation, goog.events.EventType.CLICK,
      function(e) {
        e.preventDefault();
        this.dispatchEvent({'type': 'addAllocation'});
      });

  this.handler_.listen(this, ['ratioChange', 'ratioAdded', 'ratioDeleted'],
                       this.onDataChange_);
  var selectors = this.dom_.getElementsByClass('js-bucket-select', this.container);
  for (var i = 0; i < selectors.length; i++) {
    this.handler_.listen(selectors[i], goog.events.EventType.FOCUS, function(e){
      this.reloadSelectorOptions_(e.target);
    })
  }
};


/**
 *
 * @param {Element} row Row.
 * @param {number} rangeIndex Range index.
 * @private
 */
indeed.proctor.editor.AllocationEditor.prototype.bindRow_ =
    function(row, rangeIndex) {
  var select = this.dom_.getElementsByTagNameAndClass(goog.dom.TagName.SELECT,
                                                      null, row)[0];
  var input = this.dom_.getElementsByTagNameAndClass(goog.dom.TagName.INPUT,
                                                     null, row)[0];
  var onUpdate = goog.partial(this.onRatioChange_,
                              rangeIndex, select, input, row);

  this.handler_.listen(select, goog.events.EventType.CHANGE, onUpdate);
  this.handler_.listen(input, goog.events.EventType.CHANGE, onUpdate);

  var deleteBucket = this.dom_.getElementsByTagNameAndClass(null,
                                                            'js-delete-range',
                                                            row);
  for (var i = 0; i < deleteBucket.length; i++) {
    this.handler_.listen(deleteBucket[i], goog.events.EventType.CLICK,
                         goog.partial(this.onDeleteRange_, rangeIndex, row));
  }
  var splitBucket = this.dom_.getElementsByTagNameAndClass(null,
                                                           'js-split-range',
                                                           row);
  for (var i = 0; i < splitBucket.length; i++) {
    this.handler_.listen(splitBucket[i],
                         goog.events.EventType.CLICK,
                         goog.partial(this.onSplitRange_, rangeIndex, row));
  }
};


/**
 *
 * @param {number} rangeIndex Range index.
 * @param {Element} select Bucket select.
 * @param {Element} input Value input.
 * @param {Element} row Row.
 * @param {goog.events.BrowserEvent} ev Event Facade.
 * @private
 */
indeed.proctor.editor.AllocationEditor.prototype.onRatioChange_ =
    function(rangeIndex, select, input, row, ev) {
  var nValue;
  indeed.foundation.forms.removeError(select);
  indeed.foundation.forms.removeError(input);

  // check if a number
  // check if out of the range
  input.value = this.normalizeRatio_(input.value);
  nValue = goog.string.toNumber(input.value);
  if (nValue >= 0 && nValue <= 1) {
    if (0 < nValue && nValue < 1E-4) {
      indeed.foundation.forms.addError(input, 'Should be at least 0.01%');
    } else {
      var span = this.dom_.getElementsByTagNameAndClass(goog.dom.TagName.SPAN,
          'ui-allocation-percent',
          row)[0];
      goog.dom.replaceNode(this.buildBucketRangeSpan_(rangeIndex, nValue),
          span);
      // TODO get selected value from the <select> input element
      // selectedIndex / selected, <select>.value isn't cross-browser compatible

      // convert to number via +trick
      this.ranges[rangeIndex]['length'] = goog.string.toNumber(input.value);
      this.ranges[rangeIndex]['bucketValue'] = goog.string.toNumber(select.value);

      this.dispatchEvent({
        'type': 'ratioChange', rangeIndex: rangeIndex,
        bucketValue: select.value, length: input.value
      });
    }
  } else {
    indeed.foundation.forms.addError(input, 'values in [0, 1.0]');
  }
};


/**
 *
 * @param {number} rangeIndex Range index.
 * @param {Element} row Row.
 * @param {goog.events.BrowserEvent} ev Event Facade.
 * @private
 */
indeed.proctor.editor.AllocationEditor.prototype.onDeleteRange_ =
    function(rangeIndex, row, ev) {
  ev.preventDefault();
  var range = this.ranges[rangeIndex];

  goog.array.removeAt(this.ranges, rangeIndex);

  this.dispatchEvent({'type': 'ratioDeleted', rangeIndex: rangeIndex,
    bucketValue: range['bucketValue'],
    length: range['length']});
  this.render_();
};


/**
 *
 * @param {number} rangeIndex Range index.
 * @param {Element} row Row.
 * @param {goog.events.BrowserEvent} ev Event Facade.
 * @private
 */
indeed.proctor.editor.AllocationEditor.prototype.onSplitRange_ =
    function(rangeIndex, row, ev) {
  ev.preventDefault();
  var fromrange = this.ranges[rangeIndex],
      index = rangeIndex + 1;

  var range = {
    'bucketValue': fromrange['bucketValue'],
    'length': fromrange['length'] / 2.0
  };
  fromrange['length'] = fromrange['length'] / 2.0;

  goog.array.splice(this.ranges, index, 0, range);

  this.dispatchEvent({'type': 'ratioAdded', rangeIndex: index,
    bucketValue: range['bucketValue'],
    length: range['length']});
  this.render_();

};


/**
 * Return normalized (1,100] -> (0.1.0)
 * @param {number|string} ratio Ratio between 0 and 100.
 * @return {number|string} Normalized ratio in 0, 1.0 range.
 * @private
 */
indeed.proctor.editor.AllocationEditor.prototype.normalizeRatio_ =
    function(ratio) {
  if (ratio > 1 && ratio <= 100) {
    return ratio / 100.0;
  }
  return ratio;
};


/**
 *
 * @param {goog.events.BrowserEvent} e Event Facade.
 * @private
 */
indeed.proctor.editor.AllocationEditor.prototype.onAddRatioClick_ =
    function(e) {
  e.preventDefault();
  var bucketSelect = this.dom_.getElementsByTagNameAndClass(
      goog.dom.TagName.SELECT, undefined, this.addRatioRow)[0];
  var bucketLength = this.dom_.getElementsByTagNameAndClass(
      goog.dom.TagName.INPUT, undefined, this.addRatioRow)[0];
  /** @type {string} */
  var value = goog.dom.forms.getValue(bucketSelect);
  /** @type {string} */
  var length = goog.dom.forms.getValue(bucketLength);
  this.addRatio(value, length);
};


/**
 *
 * @param {string|number} bucketValue Bucket Value.
 * @param {string|number} bucketLength Bucket Length.
 *
 */
indeed.proctor.editor.AllocationEditor.prototype.addRatio =
    function(bucketValue, bucketLength) {
  var rangeIndex = this.ranges.length;
  var bValue = bucketValue;
  var blength = this.normalizeRatio_(bucketLength);
  var rangeRow = this.buildRatioRow_(rangeIndex, bValue, blength);

  // add a range
  // Get the JSON for <select> + <input> + rangeIndex
  this.ranges.push({'length': blength, 'bucketValue': bValue});

  var allocations = this.dom_.getElementByClass('js-allocations',
                                                this.container);
  var ratioRows = this.dom_.getElementsByTagNameAndClass(undefined,
                                                         'js-ratio-row',
                                                         allocations);
  if (ratioRows.length > 0) {
    goog.dom.insertSiblingAfter(rangeRow, ratioRows[ratioRows.length - 1]);
  } else {
    goog.dom.insertChildAt(allocations, rangeRow, 0);
  }

  this.bindRow_(rangeRow, rangeIndex);
  this.dispatchEvent({'type': 'ratioAdded', rangeIndex: rangeIndex,
    bucketValue: bValue, length: blength});
};


/**
 *
 * @param {number} rangeIndex Range index/.
 * @param {number|string} bucketValue Bucket value.
 * @param {number|string} length Bucket length.
 * @private
 * @return {Element} Row element.
 */
indeed.proctor.editor.AllocationEditor.prototype.buildRatioRow_ =
    function(rangeIndex, bucketValue, length) {
  var rangeRow = goog.dom.createDom(goog.dom.TagName.DIV, {'class':
        'row js-ratio-row'});
  goog.dom.appendChild(rangeRow, goog.dom.createDom(goog.dom.TagName.DIV,
      {'class': 'six columns'},
      this.buildBucketSelector_(rangeIndex, bucketValue)));
  goog.dom.appendChild(rangeRow, goog.dom.createDom(goog.dom.TagName.DIV,
      {'class': 'two columns'},
      this.buildBucketRangeInput_(rangeIndex, length)));
  goog.dom.appendChild(rangeRow, goog.dom.createDom(goog.dom.TagName.DIV,
      {'class': 'two columns'},
      this.buildBucketRangeSpan_(rangeIndex, length)));
  goog.dom.appendChild(rangeRow, goog.dom.createDom(goog.dom.TagName.DIV,
      {'class': 'one columns'}, this.buildBucketDelete_(rangeIndex, length)));
  goog.dom.appendChild(rangeRow, goog.dom.createDom(goog.dom.TagName.DIV,
      {'class': 'one columns'}, this.buildSplit_(rangeIndex, length)));
  return rangeRow;
};


/**
 *
 * @param {goog.events.Event} ev Event facade.
 * @private
 */
indeed.proctor.editor.AllocationEditor.prototype.onDataChange_ = function(ev) {
  // redraw the Allocation Bar UI
  this.redrawAllocationBar_();
};


/**
 * @private
 */
indeed.proctor.editor.AllocationEditor.prototype.redrawAllocationBar_ =
    function() {
  var bar = this.dom_.getElementByClass('js-new-allocation-bar',
                                        this.container);
  goog.dom.replaceNode(this.buildAllocationBar_(), bar);
};


/**
 *
 * @param {number} rangeIndex Range index.
 * @param {number|string} value Length of range.
 * @return {Element} Selector.
 * @private
 */
indeed.proctor.editor.AllocationEditor.prototype.buildBucketSelector_ =
    function(rangeIndex, value) {
  var df = goog.dom.createDom(goog.dom.TagName.SELECT,
                              {'name': this.getRangeIndexPath(rangeIndex) +
            '.bucketValue', 'class': 'json js-bucket-select' });
  this.handler_.listen(df,  goog.events.EventType.FOCUS, function(e){
    this.reloadSelectorOptions_(e.target);
  });
  goog.dom.appendChild(df, this.buildBucketOptions_(value));
  return goog.dom.createDom(goog.dom.TagName.SPAN, {'class': 'inline'}, df);
};

indeed.proctor.editor.AllocationEditor.prototype.reloadSelectorOptions_ = function(selector) {
  var oldvalue = parseInt(goog.dom.forms.getValue(selector));
  var newoptions =
      this.buildBucketOptions_(oldvalue);
  goog.dom.removeChildren(selector);
  goog.dom.appendChild(selector, newoptions);
};

/**
 *
 * @param {number|string} value Selected bucket value.
 * @return {Element} Builds bucket select element.
 * @private
 */
indeed.proctor.editor.AllocationEditor.prototype.buildBucketOptions_ =
    function(value) {
  /** @type {Element} */
  var df = document.createDocumentFragment();
  for (var i = 0; i < this.buckets.length; i++) {
    var bucket = this.buckets[i];
    var selected = bucket['value'] == value;
    var attrs = {'value': bucket['value']};
    if (selected) {
      attrs['selected'] = 'selected';
    }
    var option = goog.dom.createDom(goog.dom.TagName.OPTION, attrs);
    option.text = bucket['name']; // escape inner html
    goog.dom.appendChild(df, option);
  }
  return /** @type {Element} */ (df);
};


/**
 *
 * @param {number} rangeIndex Range index.
 * @param {number|string} value Length of range.
 * @return {Element} Bucket value input.
 * @private
 */
indeed.proctor.editor.AllocationEditor.prototype.buildBucketRangeInput_ =
    function(rangeIndex, value) {
  return goog.dom.createDom(goog.dom.TagName.INPUT, {'value': value,
    'type': 'text', 'name': this.getRangeIndexPath(rangeIndex) + '.length',
    'class': 'json' });
};


/**
 *
 * @param {number} rangeIndex Range index.
 * @param {number|string} value Length of range.
 * @return {Element} Range length input.
 * @private
 */
indeed.proctor.editor.AllocationEditor.prototype.buildBucketRangeSpan_ =
    function(rangeIndex, value) {
  var span = goog.dom.createDom(goog.dom.TagName.SPAN,
                                {'class': 'inline ui-allocation-percent'});
  var percent = indeed.proctor.editor.AllocationEditor.FormatPercent(value);
  span.innerHTML = percent;
  return span;
};


/**
 * Really dumb percent formatting.
 * @param {number|string} value Value to format.
 * @return {string} Formatted percent.
 */
indeed.proctor.editor.AllocationEditor.FormatPercent = function(value) {
  var percent = value * 100.0 + '';
  var dotIndex = percent.indexOf('.');
  // primitive format?
  return percent.substring(0,
                           dotIndex > 0 ? dotIndex + 4 : percent.length) + '%';
};


/**
 * @return {string} String representing the JSON path to the range index.
 * @param {number} rangeIndex Range index.
 */
indeed.proctor.editor.AllocationEditor.prototype.getRangeIndexPath =
    function(rangeIndex) {
  return 'allocations[' + this.index + '].ranges[' + rangeIndex + ']';
};


/**
 * @return {Element} Renders and returns the bar representing the allocations.
 * @private
 */
indeed.proctor.editor.AllocationEditor.prototype.buildAllocationBar_ =
    function() {
  var df = goog.dom.createDom(goog.dom.TagName.DIV,
      {'class': 'ui-allocation-bar js-new-allocation-bar'});
  var sum = 0;
  var totalTestAllocationMap = {};
  for (var i = 0; i < this.ranges.length; i++) {
    var bucketValue = goog.string.toNumber(this.ranges[i]['bucketValue']);
    var bucketAllocation = goog.string.toNumber(this.ranges[i]['length']);
    if (! totalTestAllocationMap.hasOwnProperty(bucketValue)) {
      totalTestAllocationMap[bucketValue] = 0;
    }
    totalTestAllocationMap[bucketValue] =
        totalTestAllocationMap[bucketValue] + bucketAllocation;
  }
  for (var i = 0; i < this.ranges.length; i++) {
    var range = this.ranges[i];
    var bucketValue = goog.string.toNumber(range['bucketValue']);
    var length = goog.string.toNumber(range['length']);
    if (length <= 0 || length > 1) {
      continue;
    }
    sum += length;
    var percent = indeed.proctor.editor.AllocationEditor.FormatPercent(length);
    var span = goog.dom.createDom(goog.dom.TagName.SPAN,
        {'class': 'ui-allocation-range', 'style': 'width: ' + percent});
    goog.dom.classes.add(span, 'ui-color' + (1 + bucketValue) % 12);
    var label = goog.dom.createDom(goog.dom.TagName.SPAN,
        {'class': 'ui-allocation-range-lbl'});
    // get bucket by bucket value
    var bucket = this.getBucketByValue_(bucketValue, this.buckets);
    label.innerHTML = goog.string.htmlEscape(bucket['name'] + ' - ' + percent, false);
    goog.dom.appendChild(span, label);
    goog.dom.appendChild(df, span);
  }
  var DELTA = 1E-6,
      difference = sum - 1.0,
      warnings = [],
      errors = [];
  if (Math.abs(difference) > DELTA) {
    errors.push(indeed.proctor.editor.AllocationEditor.formatSumError(sum));
  }
  var positiveTestsSameSizeAsControl = true,
      /* The number of buckets with allocation greater than zero */
      numActiveBuckets = 0,
      hasControlBucket = totalTestAllocationMap.hasOwnProperty(0),
      totalControlBucketAllocation, totalBucketAllocation, difference;
  for (bucketValue in totalTestAllocationMap) {
    totalBucketAllocation = totalTestAllocationMap[bucketValue];
    if(totalBucketAllocation > 0) {
      numActiveBuckets++;
    }
  }

  /* if there are 2 buckets with positive allocations, test and control buckets
     should be the same size
   */
  if(numActiveBuckets > 1 && hasControlBucket) {
    totalControlBucketAllocation = totalTestAllocationMap[0];
    if(hasControlBucket) {
      for (bucketValue in totalTestAllocationMap) {
        totalBucketAllocation = totalTestAllocationMap[bucketValue];
        difference = totalBucketAllocation - totalControlBucketAllocation;
        if (bucketValue > 0 && totalBucketAllocation > 0 &&
            Math.abs(difference) >= DELTA) {
          warnings.push('Positive buckets should be the same size as the control bucket.');
          break;
        }
      }
    }
  }

  /* If there are 2 buckets with positive allocations, one should be control */
  if(numActiveBuckets > 1 && !hasControlBucket) {
    warnings.push('You should have a zero bucket (control).');
  }

  /* check if the changing allocation cause a user drift */
  if(this.checkUserDrift_(this.prevRanges, this.ranges)) {
    warnings.push(
      [
        'The allocation changing may cause a ',
        goog.dom.createDom(
          goog.dom.TagName.A,
            {'href': 'http://opensource.indeedeng.io/proctor/docs/best-practices/', 'target': '_blank'},
          'user drift'
        ),
        '.'
      ]
    )
  }

  this.displayMessages_(errors, warnings);
  return df;
};

/**
 *
 * @param {Array} prevRanges Ranges before changing.
 * @param {Array} newRanges Ranges after changing.
 * @return Boolean Whether given allocation changing contains a user drift.
 * @private
 */
indeed.proctor.editor.AllocationEditor.prototype.checkUserDrift_ =
    function(prevRanges, newRanges) {
  var boundaries = [];
  var addBoundaries = function(ranges) {
    var sum = 0;
    for (var i = 0; i < ranges.length; i++) {
      boundaries.push(sum += ranges[i]["length"]);
    }
  };
  addBoundaries(prevRanges);
  addBoundaries(newRanges);
  boundaries.sort();

  var getValue = function(ranges, pos) {
    var sum = 0;
    for (var i = 0; i < ranges.length; i++) {
      sum += ranges[i]["length"];
      if (pos < sum) {
        return ranges[i]["bucketValue"];
      }
    }
    return -1;
  };

  var left = 0.0;
  for (var i = 0; i < boundaries.length; i++) {
    right = boundaries[i];
    var len = right - left;
    if (len > 1E-4) {
      var mid = (left + right) / 2;
      var prevValue = getValue(prevRanges, mid);
      var newValue = getValue(newRanges, mid);
      if(prevValue != -1 && newValue != -1 && prevValue != newValue) {
        return true;
      }
    }
    left = right;
  }
  return false;
};

/**
 *
 * @param {string|number} bucketValue Value of bucket.
 * @param {Array} buckets Bucket array.
 * @return {Object?} Bucket if identified by value.
 * @private
 */
indeed.proctor.editor.AllocationEditor.prototype.getBucketByValue_ =
    function(bucketValue, buckets) {
  for (var i = 0; i < buckets.length; i++) {
    if (buckets[i]['value'] == bucketValue) {
      return buckets[i];
    }
  }
  return null;
};


/**
 *
 * @param {number} bucketValue Value of bucket.
 * @param {string} bucketName Name of bucket.
 * @param {Array} buckets Bucket array.
 * @return {Object?} Bucket if identified by value / name.
 * @private
 */
indeed.proctor.editor.AllocationEditor.prototype.getBucketByValueOrName_ =
    function(bucketValue, bucketName, buckets) {
  var bucket = this.getBucketByValue_(bucketValue, buckets);
  if (bucket != null) {
    return bucket;
  }
  for (var i = 0; i < buckets.length; i++) {
    if (buckets[i]['name'] == bucketName) {
      return buckets[i];
    }
  }
  return null;
};


/**
 *
 * @param {number} rangeIndex Index of range.
 * @param {number|string} value Value of range.
 * @private
 * @return {Element} Split link.
 */
indeed.proctor.editor.AllocationEditor.prototype.buildBucketDelete_ =
    function(rangeIndex, value) {
  var el = goog.dom.createDom(goog.dom.TagName.A,
      {'class': 'js-delete-range tiny button secondary radius', 'href': '#'});
  el.innerHTML = 'delete';
  return el;
};


/**
 *
 * @param {number} rangeIndex Index of range.
 * @param {number|string} value Value of range.
 * @private
 * @return {Element} Split link.
 */
indeed.proctor.editor.AllocationEditor.prototype.buildSplit_ =
    function(rangeIndex, value) {
  var el = goog.dom.createDom(goog.dom.TagName.A,
      {'class': 'js-split-range tiny button secondary radius',
        'href': '#'});
  el.innerHTML = 'split';
  return el;
};


/**
 * @return {*} Return JSON representation of JSON.
 */
indeed.proctor.editor.AllocationEditor.prototype.toJSON = function() {
  var els = goog.array.concat(
      goog.array.toArray(this.dom_.getElementsByTagNameAndClass(
          goog.dom.TagName.INPUT, 'json', this.container)),
      goog.array.toArray(this.dom_.getElementsByTagNameAndClass(
          goog.dom.TagName.SELECT, 'json', this.container))
      );
  var definition = indeed.proctor.forms.toJSON(els);
  var allocation = definition['allocations'][this.index];
  if (!goog.isDef(allocation['ranges'])) {
    allocation['ranges'] = [];
  }
  return allocation;
};


/**
 *
 * @param {Array} buckets Buckets are updated.
 */
indeed.proctor.editor.AllocationEditor.prototype.bucketsUpdated =
    function(buckets) {
  var oldbuckets = this.buckets;
  this.buckets = goog.array.clone(buckets);
  var selectors = this.dom_.getElementsByTagNameAndClass(
      goog.dom.TagName.SELECT,
      undefined, this.dom_.getElementByClass('js-allocations', this.container));
  for (var i = 0; i < selectors.length; i++) {
    var selector = selectors[i];
    var check = this.checkSelectedBucketIsChanged_(selector, oldbuckets);
    if (check['isChanged']) {
      var newbucket = check['newbucket'];
      var oldbucket = check['oldbucket'];
      /** selected buckets has been updated **/
      if (newbucket == null) {
        this.setSelectorDefault_(selector);
        indeed.foundation.forms.addError(selector,
            (oldbucket ? oldbucket['name'] : 'Unknown') + ' bucket deleted');
      } else {
        goog.dom.forms.setValue(selector, newbucket['value'].toString());
        this.reloadSelectorOptions_(selector);
      }
      // Update the ranges based on the new JSON from all the updated selectors
      this.ranges = this.toJSON()['ranges'];
      this.redrawAllocationBar_();
    }
  }

  var addSelector = this.dom_.getElementsByTagNameAndClass(
      goog.dom.TagName.SELECT, undefined, this.addRatioRow)[0];
  var check = this.checkSelectedBucketIsChanged_(addSelector, oldbuckets);
  if (check['isChanged']) {
    this.setSelectorDefault_(addSelector);
  }
};

indeed.proctor.editor.AllocationEditor.prototype.checkSelectedBucketIsChanged_ = function(selector, oldBuckets) {
  var oldvalue = goog.dom.forms.getValue(selector);
  var oldbucket = this.getBucketByValue_(oldvalue, oldBuckets) ||
      {'value': -9999, 'name': 'Unknown'};
  var newbucket = this.getBucketByValueOrName_(oldbucket['value'],
      oldbucket['name'], this.buckets);
  return {
    oldbucket: oldbucket,
    newbucket: newbucket,
    isChanged: newbucket == null || !goog.object.equals(oldbucket, newbucket)
  };
};

indeed.proctor.editor.AllocationEditor.prototype.setSelectorDefault_ = function(selector) {
  if (this.buckets.length > 0) {
    var defaultBucket = this.buckets[0];
    goog.dom.forms.setValue(selector, defaultBucket['value'].toString());
  }
};

/**
 * Render the widget into 'js-allocations'.
 * @private
 */
indeed.proctor.editor.AllocationEditor.prototype.render_ = function() {
  var df = goog.dom.createDom(goog.dom.TagName.DIV,
                              {'class': 'js-allocations'});
  for (var i = 0; i < this.ranges.length; i++) {
    var range = this.ranges[i],
        row = this.buildRatioRow_(i, range['bucketValue'], range['length']);
    goog.dom.appendChild(df, row);
  }
  this.redrawAllocationBar_();

  var js_allocations = this.dom_.getElementByClass('js-allocations',
                                                   this.container);
  goog.dom.replaceNode(df, js_allocations);

  this.handler_.removeAll();
  // rebind events after removing all the above listeners
  this.bind_();
};


/**
 * @return {boolean} Flag indicating if this widget is valid.
 */
indeed.proctor.editor.AllocationEditor.prototype.validate = function() {
  var json = this.toJSON();
  var isValid = true;
  if (!this.isDefault) {
    if (goog.string.isEmptySafe(json['rule'])) {
      indeed.foundation.forms.addError(
          goog.dom.getElementByClass('js-input-rule', this.container),
          'Rule cannot be empty');
      isValid = false;
    } else {
      indeed.foundation.forms.removeError(
          goog.dom.getElementByClass('js-input-rule', this.container));
    }
  }
  var ranges = json['ranges'];
  if (!goog.isArray(ranges)) {
    this.displayError_('No bucket ranges specified');
    return false;
  }
  var sum = 0,
      length,
      bucketValue,
      bucket;
  for (var i = 0; i < ranges.length; i++) {
    length = goog.string.toNumber(ranges[i]['length']);
    if (length >= 0) {
      sum += length;
      if (0 < length && length < 1E-4) {
        this.displayError_('Positive bucket length must be at least 0.01%');
        return false;
      }
    } else {
      bucketValue = goog.string.toNumber(ranges[i]['bucketValue']);
      bucket = this.getBucketByValue_(bucketValue, this.buckets);
      this.displayError_("Non-positive or empty bucket length '" +
          ranges[i]['length'] + "' found for " +
          (bucket ? bucket['name'] : 'unknown bucket ' + bucketValue));
      return false;
    }
  }
  if (Math.abs(1.0 - sum) > 1E-6) {
    this.displayError_(
        indeed.proctor.editor.AllocationEditor.formatSumError(sum));
    isValid = false;
  }
  return isValid;
};

/**
 * @return {boolean} Flag indicating if this widget is active.
 */
indeed.proctor.editor.AllocationEditor.prototype.checkActive = function () {
  return this.toJSON().ranges.some(function (range) {
    const length = goog.string.toNumber(range.length);
    return length !== 0 && length !== 1.0;
  });
};

/**
 *
 * @param {number} sum The sum of the allocations.
 * @return {string} Formatting error string.
 */
indeed.proctor.editor.AllocationEditor.formatSumError = function(sum) {
  return 'Allocation sum must be exactly 1.0 : sum = ' +
      indeed.proctor.editor.AllocationEditor.FormatPercent(sum) +
      ' difference = ' +
      indeed.proctor.editor.AllocationEditor.FormatPercent(1.0 - sum);
};


/**
 * @param {...string} var_args Error messages to display.
 * @private
 */
indeed.proctor.editor.AllocationEditor.prototype.displayError_ =
    function(var_args) {
  this.displayMessages_(arguments);
};


/**
 * @param {Array.<string>=} opt_errors Error messages to display.
 * @param {Array.<string>=} opt_warnings Warning messages to display.
 * @private
 */
indeed.proctor.editor.AllocationEditor.prototype.displayMessages_ =
    function(opt_errors, opt_warnings) {
  var showcontainer = false,
      df = goog.dom.getDocument().createDocumentFragment(),
      container = goog.dom.getElementByClass('allocations-msg-container',
                                             this.container);
  if(opt_errors) {
    goog.array.forEach(opt_errors, function(msg) {
      goog.dom.appendChild(df,
                           goog.dom.createDom(goog.dom.TagName.DIV,
                                              'alert-box alert', msg));
    });
    showcontainer = opt_errors.length > 0;
  }
  if(opt_warnings) {
    goog.array.forEach(opt_warnings, function(msg) {
      goog.dom.appendChild(df,
                           goog.dom.createDom(goog.dom.TagName.DIV,
                                              'alert-box secondary warning', msg));
    });
    showcontainer |= opt_warnings.length > 0;
  }

  goog.style.showElement(container, false);
  // no shorthand for : goog.dom.replaceChildren(container, df)
  goog.dom.removeChildren(container);
  goog.dom.appendChild(container, df);
  goog.style.showElement(container, showcontainer);
};
