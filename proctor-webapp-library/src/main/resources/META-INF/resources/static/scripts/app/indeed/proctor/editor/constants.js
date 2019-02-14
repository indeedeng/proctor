goog.provide('indeed.proctor.editor.ConstantsEditor');

goog.require('goog.dom');
goog.require('goog.events');
goog.require('goog.events.EventHandler');
goog.require('goog.events.EventTarget');
goog.require('goog.format.JsonPrettyPrinter');
goog.require('goog.string');
goog.require('indeed.proctor.forms');



/**
 *
 * @param {Element} container Root element.
 * @param {Object} definition JSON definition.
 * @param {boolean} isCreate Is this a new test.
 * @constructor
 * @extends {goog.events.EventTarget}
 */
indeed.proctor.editor.ConstantsEditor =
    function(container, definition, isCreate) {
  goog.base(this);
  /** @type {Element} */
  this.container = container;
  /** @type {goog.dom.DomHelper} @private */
  this.dom_ = goog.dom.getDomHelper(container);
  /** @type {goog.events.EventHandler} @private */
  this.handler_ = new goog.events.EventHandler(this);

  /** @type {goog.format.JsonPrettyPrinter} @private */
  this.prettyPrinter_ = new goog.format.JsonPrettyPrinter(null);

  this.bind_();

  /** @type {Object} */
  this.constants = goog.array.clone(definition[this.jsonEditor_.name]);

};
goog.inherits(indeed.proctor.editor.ConstantsEditor, goog.events.EventTarget);


/**
 * Write out JSON-pretty-private constants into the text-editor
 * @private
 */
indeed.proctor.editor.ConstantsEditor.prototype.prettyPrintConstants_ =
    function() {

  // remove the children - parse itself as JSON
  var json = this.toJSON();

  if (json != null) {
    this.jsonEditor_.value = this.prettyPrinter_.format(json);
  } else {
    this.displayError_('Could not parse json');
  }

};


/**
 * Identify elements within container + add event listeners
 * @private
 */
indeed.proctor.editor.ConstantsEditor.prototype.bind_ = function() {
  this.jsonEditor_ = this.dom_.getElementByClass('js-edit-constants',
                                                 this.container);

  this.handler_.listen(this.jsonEditor_, goog.events.EventType.CHANGE,
                       this.onConstantsChange_);
  this.handler_.listen(this, 'constantsChanged', function() { this.render_() });
};


/**
 * @return {Object} JSON representation.
 */
indeed.proctor.editor.ConstantsEditor.prototype.toJSON = function() {
  try {
    // try to parse constants as json
    var json = indeed.proctor.forms.toJSON([this.jsonEditor_]);
    return json[this.jsonEditor_.name];
  } catch (e) {
    return null;
  }
};


/**
 * @return {boolean} Flag whose value indicates if the constants are valid JSON.
 */
indeed.proctor.editor.ConstantsEditor.prototype.validate = function() {
  var json = this.toJSON();
  if (!this.validateConstants_(json)) {
    return false;
  } else {
    return true;
  }
};


/**
 *
 * @param {string} variable Name of the variable.
 * @param {*} value Value of the variable.
 * @private
 * @return {Element} Element for Row.
 */
indeed.proctor.editor.ConstantsEditor.prototype.buildConstantRow_ =
    function(variable, value) {
  var rangeRow = goog.dom.createDom(goog.dom.TagName.DIV,
                                    {'class': 'row js-constant-row'});
  goog.dom.appendChild(rangeRow, goog.dom.createDom(goog.dom.TagName.DIV,
      {'class': 'six columns'},
      this.buildVariableLabel_(variable, value)));
  goog.dom.appendChild(rangeRow, goog.dom.createDom(goog.dom.TagName.DIV,
      {'class': 'six columns'}, this.buildValueLabel_(variable, value)));
  return rangeRow;
};


/**
 *
 * @param {string} variable Variable name.
 * @param {*} value Value of variable.
 * @return {Element} Label.
 * @private
 */
indeed.proctor.editor.ConstantsEditor.prototype.buildValueLabel_ =
    function(variable, value) {
  var label = goog.dom.createDom(goog.dom.TagName.LABEL, {'class': 'inline' });
  label.innerHTML = goog.string.htmlEscape(value);
  return label;
};


/**
 *
 * @param {string} variable Variable name.
 * @param {*} value Value of variable.
 * @return {Element} Label.
 * @private
 */
indeed.proctor.editor.ConstantsEditor.prototype.buildVariableLabel_ =
    function(variable, value) {
  var label = goog.dom.createDom(goog.dom.TagName.LABEL, {'class': 'inline' });
  label.innerHTML = goog.string.htmlEscape(variable);
  return label;
};


/**
 * Render constants into 'js-constants'
 * @private
 */
indeed.proctor.editor.ConstantsEditor.prototype.render_ = function() {
  var df = goog.dom.createDom(goog.dom.TagName.DIV, {'class': 'js-constants'});
  var keys = indeed.proctor.editor.ConstantsEditor.getKeys(this.constants);
  for (var i = 0; i < keys.length; i++) {
    var variable = keys[i],
        value = this.constants[variable],
        row = this.buildConstantRow_(variable, value);
    goog.dom.appendChild(df, row);
  }

  var js_buckets = this.dom_.getElementByClass('js-constants', this.container);
  goog.dom.replaceNode(df, js_buckets);

  this.prettyPrintConstants_();

  this.handler_.removeAll();
  // rebind events after removing all the above listeners
  this.bind_();
};


/**
 *
 * @param {goog.events.Event} ev Event Facade.
 * @private
 */
indeed.proctor.editor.ConstantsEditor.prototype.onConstantsChange_ =
    function(ev) {
  var constants = this.toJSON();

  if (!this.validateConstants_(constants)) {
    return;
  }
  this.constants = constants;
  this.dispatchEvent({'type': 'constantsChanged', constants: this.constants});
};


/**
 *
 * @param {string} error Message to display.
 * @private
 */
indeed.proctor.editor.ConstantsEditor.prototype.displayError_ =
    function(error) {
  var container = goog.dom.getElementByClass('constants-msg-container',
                                             this.container);
  container.innerHTML = goog.string.htmlEscape(error, false);
  goog.dom.classes.addRemove(container, 'success', 'alert');
  goog.style.showElement(container, true);
};


/**
 * @private
 */
indeed.proctor.editor.ConstantsEditor.prototype.hideError_ = function() {
  var container = goog.dom.getElementByClass('constants-msg-container',
                                             this.container);
  goog.style.showElement(container, false);
};


/**
 *
 * @param {*} constants Constants to validate.
 * @return {boolean} Flag whose value indicates if the constants are valid.
 * @private
 */
indeed.proctor.editor.ConstantsEditor.prototype.validateConstants_ =
    function(constants) {
  if (goog.isDefAndNotNull(constants) &&
      goog.isObject(constants) &&
      !goog.isArray(constants) &&
      !goog.isFunction(constants)) {
    // check that each key doesn't have any non-alphanumeric characters
    var keys = indeed.proctor.editor.ConstantsEditor.getKeys(constants);
    for (var i = 0; i < keys.length; i++) {
      if (goog.string.isEmpty(keys[i])) {
        indeed.foundation.forms.addError(this.jsonEditor_,
                                         'An empty key was provided');
        return false;
      }
      if (!this.validVariableName(keys[i])) {
        indeed.foundation.forms.addError(this.jsonEditor_,
            'Variable names should be alpha-numeric + underscore. Found: \'' +
            keys[i] + '\'');
        return false;
      }
    }
    indeed.foundation.forms.removeError(this.jsonEditor_);
    return true;
  } else {
    indeed.foundation.forms.addError(this.jsonEditor_,
        'Please enter valid JSON representing a JSON object');
    return false;
  }
};


/**
 *
 * @param {string} str Variable name.
 * @return {boolean} Flag whose value indicates if this variable name is valid.
 */
indeed.proctor.editor.ConstantsEditor.prototype.validVariableName =
    function(str) {
  return !/[^a-zA-Z0-9_]/.test(str);
};


/**
 *
 * @param {Object} obj Object.
 * @return {Array.<string>} Returns array of keys for a given object.
 */
indeed.proctor.editor.ConstantsEditor.getKeys = function(obj) {
  var keys = [];
  for (var key in obj) {
    if (obj.hasOwnProperty(key)) {
      keys.push(key);
    }
  }
  return keys;
};
