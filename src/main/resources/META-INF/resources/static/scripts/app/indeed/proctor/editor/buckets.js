goog.provide('indeed.proctor.editor.BucketsEditor');

goog.require('goog.dom');
goog.require('goog.events');
goog.require('goog.events.EventHandler');
goog.require('goog.events.EventTarget');
goog.require('goog.math');
goog.require('goog.string');
goog.require('indeed.proctor.forms');



/**
 *
 * @param {Element} container Root Element.
 * @param {Object} definition JSON definition.
 * @param {boolean} isCreate Is new test.
 * @constructor
 * @extends {goog.events.EventTarget}
 */
indeed.proctor.editor.BucketsEditor =
    function(container, definition, isCreate) {
  goog.base(this);
  /** @type {Element} Root Element */
  this.container = container;
  /** @type {goog.dom.DomHelper} @private */
  this.dom_ = goog.dom.getDomHelper(container);
  /** @type {goog.events.EventHandler} @private */
  this.handler_ = new goog.events.EventHandler(this);

  this.jsonSerializer_ = new goog.json.Serializer();

  /** @type {Array.<{name:string, value:number, description:string, payload:Object}>} Array of
   * bucket values */
  this.buckets = goog.array.clone(definition['buckets']);

  this.bucketsRemoved = false;
  this.bucketsAdded = false;
  this.originalBucketLength = this.buckets.length;

  this.bind_();
};
goog.inherits(indeed.proctor.editor.BucketsEditor, goog.events.EventTarget);


/**
 * Identify elements within container + add event listeners
 * @private
 */
indeed.proctor.editor.BucketsEditor.prototype.bind_ = function() {
  var payload_selector = this.dom_.getElementByClass('js-payload-type', this.container);
  this.handler_.listen(payload_selector, goog.events.EventType.CHANGE, this.onPayloadTypeChange_);
  /** @type {string} */
  this.payloadType = payload_selector.value;

  this.addBucketRow = this.dom_.getElementByClass('js-add-bucket-row',
                                                  this.container);
  var addBucket = this.dom_.getElementsByTagNameAndClass(null, 'js-add-bucket',
                                                         this.container);

  for (var i = 0; i < addBucket.length; i++) {
    this.handler_.listen(addBucket[i], goog.events.EventType.CLICK,
                         this.onAddBucketClick_);
    goog.dom.classes.enable(addBucket[i], 'disabled', this.bucketsRemoved);
  }
  var el_value = this.dom_.getElementByClass('js-bucket-value',
                                             this.addBucketRow),
      el_name = this.dom_.getElementByClass('js-bucket-name',
                                            this.addBucketRow),
      el_description = this.dom_.getElementByClass('js-bucket-description',
                                                   this.addBucketRow),
      el_payload = this.dom_.getElementByClass('js-bucket-payload',
                                             this.addBucketRow);
    var onChange = function() {
    var value = +goog.dom.forms.getValue(el_value);
    this.validateBucket_(value, goog.dom.forms.getValue(el_name),
                         goog.dom.forms.getValue(el_description),
                         goog.dom.forms.getValue(el_payload), this.payloadType, -1,
                         el_value, el_name, el_description, el_payload);
  }
  this.handler_.listen(el_value, goog.events.EventType.CHANGE, onChange);
  this.handler_.listen(el_name, goog.events.EventType.CHANGE, onChange);
  this.handler_.listen(el_description, goog.events.EventType.CHANGE, onChange);
  this.handler_.listen(el_payload, goog.events.EventType.CHANGE, onChange);

  var rows = this.dom_.getElementsByTagNameAndClass(null, 'js-bucket-row',
                                                    this.container);
  for (var i = 0; i < rows.length; i++) {
    this.bindRow_(rows[i], i);
  }
};


/**
 * Click handler on Add Bucket.
 * @param {goog.events.BrowserEvent} e Event facade.
 * @private
 */
indeed.proctor.editor.BucketsEditor.prototype.onAddBucketClick_ = function(e) {
  e.preventDefault();

  var json = this.getBucketJsonByRow_(this.addBucketRow, this.payloadType);
  var bucket = json['add-bucket'];
  // coerce value to number
  bucket['value'] = goog.string.toNumber(bucket['value']);
  if (!this.bucketsRemoved) {  //add if any original bucket was not removed
    if (this.addBucket(bucket['value'], bucket['name'], bucket['description'], bucket['payload'])) {
      this.bucketsAdded = true;
      indeed.foundation.forms.addError(
              this.dom_.getElementByClass('js-buckets', this.container),
              'You added a new bucket and can no longer delete any original bucket.');
      this.render_();
    }
  }
};


/**
 * This turns the various text field values into a
 * @param {Element} bucketRow Row containing the bucket inputs.
 * @return {Object} JSON representing the bucket.
 * @private
 */
indeed.proctor.editor.BucketsEditor.prototype.getBucketJsonByRow_ =
    function(bucketRow, payloadType) {
  var els = null;
  if ('none' == payloadType) {
      // If our drop-down for Payload type is set to "none", we completely ignore the payload field.
      var els = goog.array.concat(
              goog.array.toArray(this.dom_.getElementsByTagNameAndClass(goog.dom.TagName.INPUT, null, bucketRow)));
  } else {
      // We have a payload type.  Include the payload TEXTAREAs as well.
      var els = goog.array.concat(
              goog.array.toArray(this.dom_.getElementsByTagNameAndClass(goog.dom.TagName.INPUT, null, bucketRow)),
              goog.array.toArray(this.dom_.getElementsByTagNameAndClass(goog.dom.TagName.TEXTAREA, null, bucketRow))
      );
  }
  return indeed.proctor.forms.toJSON(els);
};


/**
 * Adds a new bucket with the given value, name, description if valid.
 *
 * @param {string|number} value Value of the bucket.
 * @param {string} name Name of the bucket.
 * @param {string} description Description of the bucket.
 * @param {Object} payload JSON fragment for untyped payload contents of the bucket
 * @return {boolean} True if the payload is valid and the add worked.
 */
indeed.proctor.editor.BucketsEditor.prototype.addBucket =
    function(value, name, description, payload) {
  var newIndex = this.buckets.length;
  var el_value = this.dom_.getElementByClass('js-bucket-value',
                                             this.addBucketRow),
      el_name = this.dom_.getElementByClass('js-bucket-name',
                                            this.addBucketRow),
      el_description = this.dom_.getElementByClass('js-bucket-description',
                                                   this.addBucketRow),
      el_payload = this.dom_.getElementByClass('js-bucket-payload',
                  this.addBucketRow);
  var payload_type = this.payloadType;

  // Convert JSON payload fragment back into a string so it can be re-parsed by validateBucket_().  (Yea, I know...)
  var payloadBuffer = new goog.string.StringBuffer();
  this.ppObject_(payload, payloadBuffer);
  if (!this.validateBucket_(value, name, description, payloadBuffer.toString(), payload_type, -1,
                            el_value, el_name, el_description, el_payload)) {
    return false;
  }

  var fullPayload = this.makeFullPayload_(payload, payload_type);
  var bucketRow = this.buildBucketRow_(newIndex, value,
                                       name, description, fullPayload, payload_type, false);
  this.buckets.push({'value': value, 'name': name, 'description': description, 'payload': fullPayload});

  goog.dom.appendChild(
      this.dom_.getElementByClass('js-buckets', this.container), bucketRow);

  this.bindRow_(bucketRow, newIndex);

  this.dispatchEvent({'type': 'bucketAdded', buckets: this.buckets,
    bucketIndex: newIndex, bucketValue: value,
    bucketName: name, bucketDescription: description, bucketPayload: payload});
  return true;
};


/**
 *
 * Validates bucket modifications + edits.
 * Prevents duplicate/empty values, duplicate/empty names
 *
 * @param {string|number} value Value of the bucket.
 * @param {string} name Number of the bucket.
 * @param {string} description Description of the bucket.
 * @param {string} payload Raw string JSON representation of untyped payload fragment of the bucket.
 * @param {string} payload_type string the Proctor payload type of the payload fragment.
 * @param {number} bucketIndex Optional index that you are validating.
 * If greater than one, you are validating changes to that bucket index.
 * @param {Element|Node} el_value Input corresponding to the value.
 * @param {Element|Node} el_name Input corresponding to the name.
 * @param {Element|Node} el_description Input corresponding to the description.
 * @param {Element|Node} el_payload Input corresponding to the payload.
 * @return {boolean} Flag whose value indicates if this bucket value + index,
 * combination is valid.
 * @private
 */
indeed.proctor.editor.BucketsEditor.prototype.validateBucket_ =
    function(value, name, description, payload, payload_type, bucketIndex,
             el_value, el_name, el_description, el_payload) {
  indeed.foundation.forms.removeError(el_value);
  indeed.foundation.forms.removeError(el_name);
  indeed.foundation.forms.removeError(el_description);
  indeed.foundation.forms.removeError(el_payload);

  if (!goog.isNumber(value)) {
    indeed.foundation.forms.addError(el_value, 'Bucket value must be a number');
    return false;
  }
  if (goog.string.isEmptySafe(name)) {
    indeed.foundation.forms.addError(el_name, 'Bucket name must not be blank');
    return false;
  }
  if (!this.validBucketNameCharacters_(name)) {
    indeed.foundation.forms.addError(el_name,
                                     'Bucket name should be alpha-numeric');
    return false;
  }
  if ('none' != payload_type) {
    if (goog.string.isEmptySafe(payload)) {
      indeed.foundation.forms.addError(el_payload, 'Payload must be supplied.');
      return false;
    }
    if (!this.validPayloadStringForType_(payload, payload_type, el_payload)) {
      // validPayloadStringForType_ will have supplied a detailed error on el_payload.
      return false;
    }
  }
  // check if there is another bucket with the same name
  for (var i = 0; i < this.buckets.length; i++) {
    if (i == bucketIndex) { continue; }
    if (name == this.buckets[i]['name']) {
      indeed.foundation.forms.addError(el_name,
          'Bucket name cannot be the same as bucket[' + i + ']');
      return false;
    }
    if (value == this.buckets[i]['value']) {
      indeed.foundation.forms.addError(el_value,
          'Bucket value cannot be the same as bucket[' + i + ']');
      return false;
    }
  }
  return true;
};


/**
 *
 * @param {string} str Variable name.
 * @return {boolean} Flag whose value indicates if this variable name is valid.
 * @private
 */
indeed.proctor.editor.BucketsEditor.prototype.validBucketNameCharacters_ =
    function(str) {
  return !/[^a-zA-Z0-9_]/.test(str);
};

/**
 * Check that a payload string is valid for the particular payloadType.
 * @param {string} str Variable name.
 * @param {string} payloadType
 * @param {Element|Node} el_payload Input corresponding to the payload Element.
 * @return {boolean} Flag whose value indicates if this variable name is valid.
 * @private
 */
indeed.proctor.editor.BucketsEditor.prototype.validPayloadStringForType_ =
    function(str, payloadType, el_payload) {
        var value = null;
        try {
            value = goog.json.parse(str);
        } catch (e) {
            value = null;
        }
        // If we either parsed nothing or caught an error, we couldn't parse it at all.
        if (goog.string.isEmptySafe(value)) {
            indeed.foundation.forms.addError(el_payload, 'Cannot parse payload.  Example: '+this.expectedExample_(payloadType));
            return false;
        }
        // Figure out if what we successfully parsed is of the right type for our current payloadType.
        var typeOf = goog.typeOf(value);
        if ('longValue' == payloadType && 'number' == typeOf && goog.math.isInt(value)) return true;
        if ('doubleValue' == payloadType && 'number' == typeOf && goog.math.isFiniteNumber(value)) return true;
        if ('stringValue' == payloadType && 'string' == typeOf) return true;
        if ('array' == typeOf) {
            var valid = true;
            for (var i = 0; i < value.length; i++) {
                var elemTypeOf = goog.typeOf(value[i]);
                valid &= (('longArray' == payloadType && 'number' == elemTypeOf && goog.math.isInt(value[i]))
                        || ('doubleArray' == payloadType && 'number' == elemTypeOf && goog.math.isFiniteNumber(value[i]))
                        || ('stringArray' == payloadType && 'string' == elemTypeOf));
            }
            if (valid) return true;
        }
        indeed.foundation.forms.addError(el_payload, 'Payload of the wrong type.  Example: '+this.expectedExample_(payloadType));
        return false;
    };

/**
 * Given a payloadType returns an example use string.
 * @param {string} payloadType
 * @returns {string}
 * @private
 */
indeed.proctor.editor.BucketsEditor.prototype.expectedExample_ =
    function(payloadType) {
  switch (payloadType) {
      case 'longValue':
          return '42';
      case 'doubleValue':
          return '5.7';
      case 'stringValue':
          return '"foo"';
      case 'longArray':
          return '[ 27, 54, 5 ]';
      case 'doubleArray':
          return '[ 3.14, -27, 50.5 ]';
      case 'stringArray':
          return '[ "foo", "bar", "baz" ]';
      case 'none':
          return '(empty string)';
      default:
          return 'unknown type '+payloadType;
  }
};


/**
 *
 * @param {number} bucketIndex Index of the bucket.
 * @param {string|number} value Value of the bucket.
 * @param {string} name Name of the bucket.
 * @param {string} description Description of the bucket.
 * @param {Object|null} payload Payload of the bucket.
 * @param {boolean} renderDeleteButton whether the delete button should
 * be rendered or not.
 * @return {Element} Returns the newly constructed row.
 * Easy drop in for soy + another template.
 * @private
 */
indeed.proctor.editor.BucketsEditor.prototype.buildBucketRow_ =
    function(bucketIndex, value, name, description, payload, payloadType, renderDeleteButton) {
  // All this fancy structure is so the Tab key goes Value -> Name -> Description -> Payload -> Delete button
  var rangeRow = goog.dom.createDom(goog.dom.TagName.DIV,
          {'class': 'row js-bucket-row'});
  var inputFieldsColumn = goog.dom.createDom(goog.dom.TagName.DIV,
          {'class': 'ten columns'});
  var firstSubRow = goog.dom.createDom(goog.dom.TagName.DIV, {'class' : 'row  '});
  goog.dom.appendChild(firstSubRow,
      goog.dom.createDom(goog.dom.TagName.DIV, {'class': 'one columns'},
      this.buildValueInput_(bucketIndex, value)));
  goog.dom.appendChild(firstSubRow,
      goog.dom.createDom(goog.dom.TagName.DIV, {'class': 'two columns'},
      this.buildNameInput_(bucketIndex, name)));
  goog.dom.appendChild(firstSubRow,
      goog.dom.createDom(goog.dom.TagName.DIV, {'class': 'nine columns'},
      this.buildDescriptionInput_(bucketIndex, description)));
  goog.dom.appendChild(inputFieldsColumn, firstSubRow);

  var secondSubRow = goog.dom.createDom(goog.dom.TagName.DIV, {'class' : 'row  '});
  //  add a 1 column spacer here.
  goog.dom.appendChild(secondSubRow,
      goog.dom.createDom(goog.dom.TagName.DIV, {'class': 'one columns'},
      null));
  goog.dom.appendChild(secondSubRow,
      goog.dom.createDom(goog.dom.TagName.DIV, {'class': 'eleven columns'},
      this.buildPayloadInput_(bucketIndex, payload, payloadType)));
  goog.dom.appendChild(inputFieldsColumn, secondSubRow);

  goog.dom.appendChild(rangeRow, inputFieldsColumn);
  var button = null;
  if (renderDeleteButton) {
      button = this.buildBucketDelete_(bucketIndex, value);
  }
  goog.dom.appendChild(rangeRow,
          goog.dom.createDom(goog.dom.TagName.DIV, {'class': 'two columns'},
                  button));
  return rangeRow;
};


/**
 *
 * @param {number} bucketIndex Index of the bucket.
 * @param {string|number} value Value of the bucket.
 * @return {Element} Bucket value input element.
 * @private
 */
indeed.proctor.editor.BucketsEditor.prototype.buildValueInput_ =
    function(bucketIndex, value) {
  return this.buildInput_(bucketIndex, value, 'value', 'Value');
};


/**
 * @param {number} bucketIndex Index of the bucket.
 * @param {string} name Name of the bucket.
 * @return {Element} Bucket name input element.
 * @private
 */
indeed.proctor.editor.BucketsEditor.prototype.buildNameInput_ =
    function(bucketIndex, name) {
  return this.buildInput_(bucketIndex, name, 'name', 'Name');
};


/**
 * @param {number} bucketIndex Index of the bucket.
 * @param {string} description Description of the bucket.
 * @return {Element} Bucket description input element.
 * @private
 */
indeed.proctor.editor.BucketsEditor.prototype.buildDescriptionInput_ =
    function(bucketIndex, description) {
  return this.buildInput_(bucketIndex, description,
                          'description', 'Description');
};


/**
 * This converts a |payload| from a JSON fragment of a payload implicitly typed by payloadType
 * to a filled-in TEXTAREA object with the right name and contents.
 *
 * @param {number} bucketIndex Index of the bucket.
 * @param {Object} payload Payload fragment for the bucket.
 * @param {string} payloadType The string type of the payload.  For example: 'stringArray'
 * @return {Element} Bucket description input element.
 * @private
 */
indeed.proctor.editor.BucketsEditor.prototype.buildPayloadInput_ =
  function(bucketIndex, payload, payloadType) {
    var valstr = this.prettyPrintPayloadValue_(payload);
    if (!goog.isDefAndNotNull(valstr)) {
        valstr = "";   // We have to supply something here regardless.
    }
    var thing = goog.dom.createDom(goog.dom.TagName.TEXTAREA, {'rows': '1', 'cols' : '11',
      'class': 'js-bucket-payload json',
      'name': this.getInputName_(bucketIndex, 'payload.'+payloadType),
      'value': valstr,
      'disabled': ('none' == payloadType)
      });
    thing.setAttribute('data-json-type', 'raw');
    return thing;
};

/**
 * Figure out the value of a payload, and pretty-print it to a string.
 * @param {Object} payload
 * @returns {string|null}
 * @private
 */
indeed.proctor.editor.BucketsEditor.prototype.prettyPrintPayloadValue_ =
  function(payload) {
    if (!goog.isDefAndNotNull(payload)) {
        return null;
    }
    var outputBuffer = new goog.string.StringBuffer();
    if (goog.isDefAndNotNull(payload['doubleValue'])) {
      this.ppObject_(payload['doubleValue'], outputBuffer);
    } else if (goog.isDefAndNotNull(payload['longValue'])) {
      this.ppObject_(payload['longValue'], outputBuffer);
    } else if (goog.isDefAndNotNull(payload['stringValue'])) {
      this.ppObject_(payload['stringValue'], outputBuffer);
    } else if (goog.isDefAndNotNull(payload['doubleArray'])) {
      this.ppObject_(payload['doubleArray'], outputBuffer);
    } else if (goog.isDefAndNotNull(payload['longArray'])) {
      this.ppObject_(payload['longArray'], outputBuffer);
    } else if (goog.isDefAndNotNull(payload['stringArray'])) {
      this.ppObject_(payload['stringArray'], outputBuffer);
    } else {
      return null;   // We don't know how to handle this type.
    }
    return outputBuffer.toString();
};

indeed.proctor.editor.BucketsEditor.prototype.ppObject_ =
  function(val, outputBuffer) {
    var typeOf = goog.typeOf(val);
    if ('array' == typeOf) {
      outputBuffer.append("[ ");
      for (var i = 0; i < val.length; i++) {
        if (i > 0) {
          outputBuffer.append(", ");
        }
        this.ppObject_(val[i], outputBuffer);
      }
      outputBuffer.append(" ]");
    } else {
      outputBuffer.append(this.jsonSerializer_.serialize(val));
    }
};

/**
 * Expand a payload fragment (e.g. [ "foo" ]) and a payload type (e.g. "stringArray")
 * into a fully-typed payload (e.g. { stringArray: [ "foo" ] }  )
 * @param {Object} payloadFragment
 * @param {string} payloadType
 * @returns {Object}
 * @private
 */
indeed.proctor.editor.BucketsEditor.prototype.makeFullPayload_ =
  function(payloadFragment, payloadType) {
      var obj = {};
      obj[payloadType] = payloadFragment;
      return obj;
};

/**
 *
 * @param {number} bucketIndex Index of the bucket.
 * @param {string|number} value Value of the input.
 * @param {string} name Name of the input.
 * @param {string} placeholder Placeholder text for the input.
 * @return {Element} Input element.
 * @private
 */
indeed.proctor.editor.BucketsEditor.prototype.buildInput_ =
    function(bucketIndex, value, name, placeholder) {
  return goog.dom.createDom(goog.dom.TagName.INPUT, {'type': 'text',
    'value': value, 'name': this.getInputName_(bucketIndex, name),
    'placeholder': placeholder, 'class': 'json js-bucket-' + name});
};


/**
 *
 * @param {number} bucketIndex Index value.
 * @param {string} field field name.
 * @return {string} Returns input name based on field-name + bucket index.
 * @private
 */
indeed.proctor.editor.BucketsEditor.prototype.getInputName_ =
    function(bucketIndex, field) {
  return 'buckets[' + bucketIndex + '].' + field;
};


/**
 *
 * @param {number} bucketIndex Index of the bucket.
 * @param {string|number} value Value of the bucket.
 * @return {Element} Returns delete element.
 * @private
 */
indeed.proctor.editor.BucketsEditor.prototype.buildBucketDelete_ =
    function(bucketIndex, value) {
  var el = goog.dom.createDom(goog.dom.TagName.A,
      {'class': 'js-delete-bucket tiny button secondary radius', 'href': '#'});
  el.innerHTML = 'Delete';
  return el;
};


/**
 * Adds event listeners to the specific row.
 * @param {Element} row Row element.
 * @param {number} bucketIndex Index of the bucket.
 * @private
 */
indeed.proctor.editor.BucketsEditor.prototype.bindRow_ =
    function(row, bucketIndex) {
  var inputs = goog.array.concat(
          goog.array.toArray(this.dom_.getElementsByTagNameAndClass(goog.dom.TagName.INPUT, null, row)),
          goog.array.toArray(this.dom_.getElementsByTagNameAndClass(goog.dom.TagName.TEXTAREA, null, row))
          );
  var onUpdate = goog.partial(this.onBucketChange_, bucketIndex, row);

  for (var i = 0; i < inputs.length; i++) {
    this.handler_.listen(inputs[i], goog.events.EventType.CHANGE, onUpdate);
  }
  var deleteBucket = this.dom_.getElementsByTagNameAndClass(null,
      'js-delete-bucket', row);
  for (var i = 0; i < deleteBucket.length; i++) {
    this.handler_.listen(deleteBucket[i], goog.events.EventType.CLICK,
                         goog.partial(this.onDeleteBucket_, bucketIndex, row));
  }
};


/**
 * @private
 * Renders the widget contents into the 'js-buckets' element under this
 * container.
 */
indeed.proctor.editor.BucketsEditor.prototype.render_ = function() {
  var df = goog.dom.createDom(goog.dom.TagName.DIV, {'class': 'js-buckets'});
  for (var i = 0; i < this.buckets.length; i++) {
    var bucket = this.buckets[i];
    var renderDeleteButton =
        (i >= this.originalBucketLength || !this.bucketsAdded);
    var row = this.buildBucketRow_(i, bucket['value'],
        bucket['name'], bucket['description'], bucket['payload'], this.payloadType, renderDeleteButton);
    goog.dom.appendChild(df, row);
  }

  var js_buckets = this.dom_.getElementByClass('js-buckets', this.container);
  goog.dom.replaceNode(df, js_buckets);

  this.handler_.removeAll();
  // rebind events after removing all the above listeners
  this.bind_();
};


/**
 *
 * @param {number} bucketIndex Bucket index to delete.
 * @param {Element} row Row that corresponds to this bucket.
 * @param {goog.events.BrowserEvent} ev Event facade.
 * @private
 */
indeed.proctor.editor.BucketsEditor.prototype.onDeleteBucket_ =
    function(bucketIndex, row, ev) {
  ev.preventDefault();
  var bucket = this.buckets[bucketIndex];

  //remove if no buckets were added, or if the bucket to delete is new
  if (!this.bucketsAdded || bucketIndex >= this.originalBucketLength) {
    goog.array.removeAt(this.buckets, bucketIndex);

    if (bucketIndex < this.originalBucketLength) {
      this.bucketsRemoved = true;
      indeed.foundation.forms.addError(
          this.dom_.getElementByClass('ui-panel-buttons', this.container),
          'You deleted an original bucket and can no longer add a new bucket.');
    }

    //if we deleted all the buckets we added
    if (this.buckets.length == this.originalBucketLength) {
      this.bucketsAdded = false;
      indeed.foundation.forms.removeError(
          this.dom_.getElementByClass('ui-panel-buttons', this.container));
    }

    this.dispatchEvent({'type': 'bucketDeleted', buckets: this.buckets,
      bucketIndex: bucketIndex, bucketValue: bucket['value'],
      bucketName: bucket['name'],
      bucketDescription: bucket['description'],
      bucketPayload: bucket['payload']});
    this.render_();
  }
};

/**
 *
 * @param {number} bucketIndex Bucket index to delete.
 * @param {Element} row Row that corresponds to this bucket.
 * @param {goog.events.BrowserEvent} ev Event facade.
 * @private
 */
indeed.proctor.editor.BucketsEditor.prototype.onBucketChange_ =
    function(bucketIndex, row, ev) {
  var el_value = this.dom_.getElementByClass('js-bucket-value', row),
      el_name = this.dom_.getElementByClass('js-bucket-name', row),
      el_description = this.dom_.getElementByClass('js-bucket-description',
                                                   row),
      el_payload = this.dom_.getElementByClass('js-bucket-payload', row);

  var value = +goog.dom.forms.getValue(el_value);
  if (!this.validateBucket_(value, goog.dom.forms.getValue(el_name),
                            goog.dom.forms.getValue(el_description),
                            goog.dom.forms.getValue(el_payload), this.payloadType, bucketIndex,
                            el_value, el_name, el_description, el_payload)) {
    return;
  }

  var json = this.getBucketJsonByRow_(row, this.payloadType);
  var bucket = json['buckets'][bucketIndex];
  bucket['value'] = +bucket['value']; // string -> number

  this.buckets[bucketIndex] = bucket;
  this.dispatchEvent({'type': 'bucketChanged', buckets: this.buckets,
    bucketIndex: bucketIndex, bucketValue: bucket['value'],
    bucketName: bucket['name'],
    bucketDescription: bucket['description'],
    bucketPayload: bucket['payload']});
};


/**
 * @return {boolean} Returns flag whose value indicates whether or not this
 * widget is valid. Checks at least one bucket, and no duplicate buckets.
 */
indeed.proctor.editor.BucketsEditor.prototype.validate = function() {
  if (this.buckets.length == 0) {
    return false;
  }
  // display an error
  var rows = this.dom_.getElementsByTagNameAndClass(null, 'js-bucket-row',
                                                    this.container);
  var isValid = true;
  for (var i = 0; i < rows.length; i++) {
    var row = rows[i];
    var el_value = this.dom_.getElementByClass('js-bucket-value', row),
        el_name = this.dom_.getElementByClass('js-bucket-name', row),
        el_description = this.dom_.getElementByClass('js-bucket-description', row),
        el_payload = this.dom_.getElementByClass('js-bucket-payload', row);
    var value = /** @type {string} */ goog.dom.forms.getValue(el_value);
    if (!goog.string.isEmptySafe(value)) {
      value = goog.string.toNumber(value); //coerce to number
    }
    var name = /** @type {string} */ goog.dom.forms.getValue(el_name);
    var description =
        /** @type {string} */ goog.dom.forms.getValue(el_description);
    var payload = /** @type {string} */ goog.dom.forms.getValue(el_payload);
    var rowValid = this.validateBucket_(value,
        name, description, payload, this.payloadType, i,
        el_value, el_name, el_description, el_payload);
    isValid = isValid && rowValid;
  }
  return isValid;
};

/**
 *
 * @param {goog.events.BrowserEvent} ev Event facade.
 * @private
 */
indeed.proctor.editor.BucketsEditor.prototype.onPayloadTypeChange_ =
  function(ev) {
    this.payloadType = ev.currentTarget.value;
    goog.dom.classes.enable(this.container, 'payloads-hidden', ('none' == this.payloadType));
    // When we change the payload type, we have to go though all the current payload fields,
    // changing the name to be appropriate so the payload will be generated with the right type,
    // and disabling the payloads if the experiment doesn't use them.
    var rows = this.dom_.getElementsByTagNameAndClass(null, 'js-bucket-row', this.container);
    for (var i = 0; i < rows.length; i++) {
      var el_payload = this.dom_.getElementByClass('js-bucket-payload', rows[i]);
      el_payload.disabled = ('none' == this.payloadType);
      // Set the name so this payload will be generated with the right payload type.
      el_payload.name = "buckets["+i+"].payload."+this.payloadType;
    }
    this.validate();

    var add_bucket_el_payload = this.dom_.getElementByClass('js-bucket-payload', this.addBucketRow);
    // re-validate the add-bucket payload if it has contents
    if ('none' != this.payloadType && !goog.string.isEmptySafe(goog.dom.forms.getValue(add_bucket_el_payload))) {
        if (this.validPayloadStringForType_(goog.dom.forms.getValue(add_bucket_el_payload), this.payloadType, add_bucket_el_payload)) {
            indeed.foundation.forms.removeError(add_bucket_el_payload);
        }
    } else {
        indeed.foundation.forms.removeError(add_bucket_el_payload);
    }

    if (!goog.string.isEmptySafe(this.buckets)) {
      // If we have buckets, we've just effectively changed all of them, so we have to say so.
      // (This will cause onBucketChange_() above to recalculate each bucket from the DOM, and pick up the
      // type change we've just made here.)
      // If too many notifications become a problem, make this be a new "bucketsChanged" event with just all the buckets.
      for (var i = 0; i < this.buckets.length; i++) {
        var bucket = this.buckets[i];
        this.dispatchEvent({'type': 'bucketChanged', buckets: this.buckets,
            bucketIndex: i, bucketValue: bucket['value'],
            bucketName: bucket['name'],
            bucketDescription: bucket['description'],
            bucketPayload: bucket['payload']});
      }
    }
};
