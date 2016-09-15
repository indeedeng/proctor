goog.provide('indeed.proctor.editor.BasicEditor');

goog.require('goog.array');
goog.require('goog.dom');
goog.require('goog.dom.forms');
goog.require('goog.events');
goog.require('goog.events.EventHandler');
goog.require('goog.events.EventTarget');
goog.require('goog.string');
goog.require('goog.style');
goog.require('indeed.foundation.forms');
goog.require('indeed.proctor.forms');



/**
 *
 * @param {Element} container Root element.
 * @param {Object} definition JSON definition.
 * @param {boolean} isCreate Flag indicating if this is a new test.
 * @constructor
 * @extends {goog.events.EventTarget}
 */
indeed.proctor.editor.BasicEditor = function(container, definition, isCreate) {
  goog.base(this);

  /** @type {Element} Container element */
  this.container = container;
  /** @type {boolean} Flag whose value indicates whether or not this is a
   * new test */
  this.isCreate = isCreate;

  /** @type {Element} name input */
  this.name = indeed.proctor.forms.getElementByInputName('testName',
      goog.dom.TagName.INPUT, null, container);
  /** @type {Element} description input */
  this.description = indeed.proctor.forms.getElementByInputName('description',
      goog.dom.TagName.INPUT, null, container);
  /** @type {Element} testType input */
  this.testType = indeed.proctor.forms.getElementByInputName('testType',
      goog.dom.TagName.SELECT, null, container);
  /** @type {Element} salt input */
  this.salt = indeed.proctor.forms.getElementByInputName('salt',
      goog.dom.TagName.INPUT, null, container);
  /** @type {Element} rule input */
  this.rule = indeed.proctor.forms.getElementByInputName('rule',
      goog.dom.TagName.INPUT, null, container);

  /**
   * @type {goog.events.EventHandler}
   * @private
   */
  this.handler_ = new goog.events.EventHandler(this);

  var onChange = function(ev) {
    indeed.proctor.forms.validateRequired(ev.currentTarget);
  };

  var updateSaltToTestName = function() {
    var name = goog.dom.forms.getValue(this.name);
    if (goog.string.isEmptySafe(name)) {
      goog.dom.forms.setValue(this.salt, '');
    } else {
      goog.dom.forms.setValue(this.salt, '&' + name);
    }
    indeed.proctor.forms.validateRequired(this.salt);
  };

  if (this.name) {
    this.handler_.listen(this.name, goog.events.EventType.CHANGE, onChange);
    this.handler_.listen(this.name, goog.events.EventType.CHANGE, updateSaltToTestName);
  }
  this.handler_.listen(this.description,
                       goog.events.EventType.CHANGE, onChange);
  this.handler_.listen(this.salt, goog.events.EventType.CHANGE, onChange);

};
goog.inherits(indeed.proctor.editor.BasicEditor, goog.events.EventTarget);


/**
 * @return {boolean} Flag whose value indicates if this widget is valid.
 */
indeed.proctor.editor.BasicEditor.prototype.validate = function() {
  var isValid = true;

  if (this.isCreate) {
    var name = goog.dom.forms.getValue(this.name);
    if (goog.string.isEmptySafe(name)) {
      indeed.foundation.forms.addError(this.name, 'Test Name cannot be empty.');
      isValid = false;
    } else if (/[^a-zA-Z0-9_]/.test(name)) {
      indeed.foundation.forms.addError(this.name,
          'Test Name must be alpha-numeric underscore.');
      isValid = false;
    } else {
      indeed.foundation.forms.removeError(this.name);
    }
  }

  var description = goog.dom.forms.getValue(this.description);
  if (goog.string.isEmptySafe(description)) {
    indeed.foundation.forms.addError(this.description,
                                     'Description cannot be empty.');
    isValid = false;
  } else {
    indeed.foundation.forms.removeError(this.description);
  }
  var salt = goog.dom.forms.getValue(this.salt);
  if (goog.string.isEmptySafe(salt)) {
    indeed.foundation.forms.addError(this.salt, 'Salt cannot be empty.');
    isValid = false;
  } else {
    indeed.foundation.forms.removeError(this.salt);
  }
  return isValid;
};


/**
 * @return {string} Returns the current test name.
 */
indeed.proctor.editor.BasicEditor.prototype.getTestName = function() {
  return /** @type {string} */ (goog.dom.forms.getValue(this.name));
};
