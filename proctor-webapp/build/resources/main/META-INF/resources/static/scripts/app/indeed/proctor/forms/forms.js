goog.provide('indeed.proctor.forms');


goog.require('goog.dom.forms');
goog.require('goog.json');
goog.require('goog.string');


/**
 * Returns a JSON representation of a list of form elements as JSON.
 * supported schemas
 *
 * x.y[0].a = "A"
 * x.y[0].b = "B"
 *
 * yields:
 * {
 *  x: {
 *    y : [
 *      {a: "A", b: "B"}
 *    ]
 * }
 *
 *
 * @param { {length: number} } inputs Array-like list of elements
 * @return {Object} JSON representation of all the input elements.
 */
indeed.proctor.forms.toJSON = function(inputs) {
  var root = {};
  for (var i = 0, len = inputs.length; i < len; i++) {
    indeed.proctor.forms.addInputToObject_(inputs[i], root);
  }
  return root;
};


/**
 *
 * @param {Element} input Input element containing data.
 * @param {Object} obj JSON Object to which to add the json for this input.
 * @private
 * @return {*} Value added to the object determined by input.
 */
indeed.proctor.forms.addInputToObject_ = function(input, obj) {
  // Handle input
  var value = goog.dom.forms.getValue(input);
  if (goog.string.isEmpty(input.name) || input.disabled || value == null) {
    return;
  }
  var isRawJson = indeed.proctor.forms.isRawJsonType_(input);
  if (isRawJson) {
    try {
      value = goog.json.parse(value);
    } catch (e) {
      return;
    }
  }
  return indeed.proctor.forms.addToObject_(input.name, value, obj);
};


/**
 * characters[::digit::] and underscore /dash
 * @type {RegExp} Regular express to match array-like input names
 */
indeed.proctor.forms.RE_ARRAY = /^([a-z\-_]+)\[([0-9]+)?\]$/i;

/**
 *
 * @type {number} A 'index' value indicating this path is not an array.
 */
indeed.proctor.forms.INDEX_NOT_ARRAY = -1;

/**
 *
 * @type {number} An 'index' value indicating you should push onto the end of
 * the array. Instead of add to a specific index.
 */
indeed.proctor.forms.INDEX_ARRAY_PUSH = -99;


/**
 *
 * @param {string} path Dot-delimited path to which to add the value.
 * @param {string|number|Array|Object|null} value Value to set at the path.
 * @param {Object} obj Object that receives the value at the given path.
 * @private
 * @return {*} Returns the value added at path.
 */
indeed.proctor.forms.addToObject_ = function(path, value, obj) {
  var parts = path.split('.'),
      part = null,
      /** @type {Object} */
      head = obj,
      field_info = null,
      pos = 0;

  for (pos = 0; pos < parts.length - 1; pos++) {
    part = goog.string.collapseWhitespace(parts[pos]);
    if (goog.string.isEmptySafe(part)) {
      throw new Error('Invalid part at position ' + pos + ' in ' + path);
    }
    field_info = indeed.proctor.forms.getFieldInfo_(part);

    // eg a.b[0].c[0]
    // will set head = a.b[0] = {}
    head = /** Object */
        indeed.proctor.forms.addToObjectFromFieldInfo_(head,
            field_info.name,
            field_info.index,
            {},
            false);
  }
  part = goog.string.collapseWhitespace(parts[pos]);
  if (goog.string.isEmptySafe(part)) {
    throw new Error('Invalid part at position ' + pos + ' in ' + path);
  }
  // final field info
  field_info = indeed.proctor.forms.getFieldInfo_(part);

  // identify the object one-level above the last field_name
  // a.b.c should leave field_name = 'c', head = a.b, field_index = -1
  // a.b[2].c should leave field_name = 'c', head = a.b[2], field_index = -1
  // a.b[1] should leave field_name = 'b', field_index = 1, head = a

  return indeed.proctor.forms.addToObjectFromFieldInfo_(head, field_info.name,
                                                        field_info.index,
                                                        value,
                                                        true);
};


/**
 *
 * @param {Element} el Input element.
 * @private
 * @return {boolean} Flag whose value indicates if this element should be
 * treated as raw JSON (using JSON.parse).
 */
indeed.proctor.forms.isRawJsonType_ = function(el) {
  var type = el.getAttribute('data-json-type');
  return 'raw' == type;
};


/**
 *
 * @param {string} path_piece String from which to parse the path information.
 * @return {{name:string, index:number}} Path information identified
 * when parsing the input piece. If not an array, returns -1 as index.
 * @private
 */
indeed.proctor.forms.getFieldInfo_ = function(path_piece) {
  var array_piece = path_piece.match(indeed.proctor.forms.RE_ARRAY);

  if (array_piece) {
    /* coerce into number */
    var index = array_piece.length > 2 && array_piece[2] ?
                goog.string.toNumber(array_piece[2]) :
                indeed.proctor.forms.INDEX_ARRAY_PUSH;
    return {
      name: array_piece[1],
      index: index
    };
  } else {
    return {
      name: path_piece,
      index: indeed.proctor.forms.INDEX_NOT_ARRAY
    };
  }
};


/**
 *
 * @param {Object} obj Object to receive value.
 * @param {string} name Name of field to use.
 * @param {number} index Index of value, if greater than 1, it is expected that
 * obj[name] is undefined or an Array.
 * @param {*} value Value to add at object at name/index position.
 * @param {boolean} overwrite Should we override field.
 * @return {Object|Array|string|number} Returns the value added at path.
 * @private
 */
indeed.proctor.forms.addToObjectFromFieldInfo_ =
    function(obj, name, index, value, overwrite) {
  var field = obj[name], actualIndex;
  if (index >= 0 || index === indeed.proctor.forms.INDEX_ARRAY_PUSH) {
    if (field !== undefined) {
      if (!goog.isArray(field)) {
        throw new Error('Invalid field: ' +
                        field + ' is not an array. From ' + name);
      }
    } else {
      obj[name] = [];
    }
    if(index === indeed.proctor.forms.INDEX_ARRAY_PUSH) {
      /* insert at end */
      actualIndex = obj[name].length;
    } else {
      actualIndex = index;
    }
    if (overwrite || obj[name][actualIndex] === undefined) {
      obj[name][actualIndex] = value;
    }
    return obj[name][actualIndex];
  } else {
    if (overwrite || obj[name] === undefined) {
      obj[name] = value;
    }
    return obj[name];
  }
};


/**
 *
 * @param {string} name Name of input-element.
 * @param {?string=} opt_tag Element tag name.
 * @param {?string=} opt_class Optional class name.
 * @param {Document|Element=} opt_el Optional element to look in.
 * @return {Array.<Element>} Array of elements matching input.name and other
 * tag, class, root element criteria.
 */
indeed.proctor.forms.getElementsByInputName =
    function(name, opt_tag, opt_class, opt_el) {
  return indeed.proctor.forms.getElementsByInputName_(name, opt_tag, opt_class);
};

/**
 *
 * @param {string} name Name of input-element.
 * @param {?string=} opt_tag Element tag name.
 * @param {?string=} opt_class Optional class name.
 * @param {Document|Element=} opt_el Optional element to look in.
 * @return {Element?} ELement that has input name.
 */

indeed.proctor.forms.getElementByInputName =
    function(name, opt_tag, opt_class, opt_el) {
  var els = indeed.proctor.forms.getElementsByInputName_(name, opt_tag,
                                                         opt_class, opt_el);
  if (els.length > 0) {
    return els[0];
  } else {
    return null;
  }
};


/**
 *
 * @param {string} name Name of input-element.
 * @param {?string=} opt_tag Element tag name.
 * @param {?string=} opt_class Optional class name.
 * @param {Document|Element=} opt_el Optional element to look in.
 * @param {number=} opt_max Maximum number of elements to find.
 * Short circuits search and will return an array of length opt_max.
 * @return {Array.<Element>} Array of elements whose name matches name,
 * and optionally match tagName, class and under root opt_el.
 * @private
 */
indeed.proctor.forms.getElementsByInputName_ =
    function(name, opt_tag, opt_class, opt_el, opt_max) {
  var els = goog.dom.getElementsByTagNameAndClass(opt_tag, opt_class, opt_el);
  var found = [];
  for (var i = 0, len = els.length; i < len; i++) {
    if (name === els[i].name) {
      found.push(els[i]);
      if (typeof opt_max === 'number' && found.length === opt_max) {
        break;
      }
    }
  }
  return found;
};


/**
 *
 * @param {Element} el Input element whose value should be non-empty.
 * @return {boolean} Flag whose value indicates if the element passed
 * validation.
 */
indeed.proctor.forms.validateRequired = function(el) {
    if (el === null) {
        return true;
    }
  var value = goog.dom.forms.getValue(el);
  if (goog.string.isEmptySafe(value)) {
    indeed.foundation.forms.addError(el, 'This field is required.');
    return false;
  } else {
    indeed.foundation.forms.removeError(el);
    return true;
  }
};
