goog.provide('indeed.proctor.editor.SvnInfoEditor');

goog.require('goog.dom');
goog.require('goog.dom.forms');
goog.require('goog.events');
goog.require('goog.events.EventHandler');
goog.require('goog.events.EventTarget');
goog.require('goog.net.XhrLite');
goog.require('goog.string');
goog.require('indeed.foundation.alerts');
goog.require('indeed.foundation.forms');
goog.require('indeed.proctor.JobMonitor');
goog.require('indeed.proctor.forms');



/**
 *
 * @param {Element} container Root element.
 * @param {boolean} shouldHandleSave Flag indicating if this should handle
 * submit event on the form using ajax.
 * @constructor
 * @extends {goog.events.EventTarget}
 */
indeed.proctor.editor.SvnInfoEditor = function(container, shouldHandleSave) {
  goog.base(this);
  this.container = container;

  if (this.container.tagName.toUpperCase() === goog.dom.TagName.FORM) {
    this.form_ = this.container;
  } else {
    this.form_ = goog.dom.getElementsByTagNameAndClass(goog.dom.TagName.FORM,
        'js-promote-definition', this.container)[0];
  }

  /**
   *
   * @type {boolean} whether or not this editor should attach to the submit
   * event .. kind of quirky but it will work for now.
   */
  this.isShouldHandleSave = !!shouldHandleSave;

  this.username = indeed.proctor.forms.getElementByInputName('username',
      null, null, container);
  this.password = indeed.proctor.forms.getElementByInputName('password',
      null, null, container);
  this.summary = indeed.proctor.forms.getElementByInputName('summary',
      null, null, container);
  this.comment = indeed.proctor.forms.getElementByInputName('comment',
      null, null, container);

  this.handler_ = new goog.events.EventHandler(this);
  this.bind_();
};
goog.inherits(indeed.proctor.editor.SvnInfoEditor, goog.events.EventTarget);


/**
 * Identify elements within container + add event listeners
 * @private
 */
indeed.proctor.editor.SvnInfoEditor.prototype.bind_ = function() {
  var onChange = function(ev) {
    indeed.proctor.forms.validateRequired(ev.currentTarget);
  };
  this.handler_.listen(this.username, goog.events.EventType.CHANGE, onChange);
  this.handler_.listen(this.password, goog.events.EventType.CHANGE, onChange);

  if (this.comment) {
    this.handler_.listen(this.comment, goog.events.EventType.CHANGE, onChange);
  }


  if (this.isShouldHandleSave) {
    this.handler_.listen(this.form_, goog.events.EventType.SUBMIT,
                         this.onFormSubmit_);
  }
};


/**
 *
 * @param {goog.events.BrowserEvent} ev Event Facade.
 * @private
 */
indeed.proctor.editor.SvnInfoEditor.prototype.onFormSubmit_ = function(ev) {
  ev.preventDefault();
  this.save_();
};


/**
 * Checks non-null fields.
 * @return {boolean} Flag whose value indicates if this editor is valid.
 */
indeed.proctor.editor.SvnInfoEditor.prototype.validate = function() {
  var valid = true;
  valid = indeed.proctor.forms.validateRequired(this.username) && valid;
  valid = indeed.proctor.forms.validateRequired(this.password) && valid;
  if (this.summary) {
    valid = indeed.proctor.forms.validateRequired(this.summary) && valid;
  }
  if (this.comment) {
    valid = indeed.proctor.forms.validateRequired(this.comment) && valid;
  }

  return valid;
};


/**
 * Save this editor.
 * @private
 */
indeed.proctor.editor.SvnInfoEditor.prototype.save_ = function() {
  var valid = this.validate();

  if (!valid) {
    return;
  }

  this.displayProgress_('Saving ...');

  goog.net.XhrLite.send(
      this.form_.action,
      goog.bind(this.onSaveCallback_, this),
      this.form_.method,
      goog.dom.forms.getFormDataString(this.form_),
      {'X-Requested-With': 'XMLHttpRequest'}
  );

};


/**
 *
 * @param {goog.events.Event} e Event Facade.
 * @private
 */
indeed.proctor.editor.SvnInfoEditor.prototype.onSaveCallback_ = function(e) {
  var xhr = /** @type {goog.net.XhrIo} */ e.target, job;
  if (xhr.isSuccess()) {
    var json = xhr.getResponseJson();
    if (json['success']) {
      // background job info.

      job = json['data'];
      if (job && job['jobId']) {
        indeed.proctor.JobMonitor.showMonitorAsPopup(job['jobId']);
        this.hideMessage_();
      } else {
        this.displaySuccess_(json['msg'] || 'Saved!');
      }

    } else {
      this.displayError_(json['msg']);
    }
  } else {
    this.displayError_(xhr.getLastError());
  }
};


/**
 *
 * @param {string} message Message to display.
 * @private
 */
indeed.proctor.editor.SvnInfoEditor.prototype.displayProgress_ =
    function(message) {
  var container = goog.dom.getElementByClass('save-msg-container',
                                             this.container);
  indeed.foundation.alerts.displayProgress(container, message);
};


/**
 *
 * @param {string} message Message to display.
 * @private
 */
indeed.proctor.editor.SvnInfoEditor.prototype.displaySuccess_ =
    function(message) {
  var container = goog.dom.getElementByClass('save-msg-container',
                                             this.container);
  indeed.foundation.alerts.displaySuccess(container, message);
};


/**
 * @private
 */
indeed.proctor.editor.SvnInfoEditor.prototype.hideMessage_ =
    function() {
  var container = goog.dom.getElementByClass('save-msg-container',
                                             this.container);
  if (container) {
    goog.style.showElement(container, false);
  }
};


/**
 *
 * @param {string} message Message to display.
 * @private
 */
indeed.proctor.editor.SvnInfoEditor.prototype.displayError_ =
    function(message) {
  var container = goog.dom.getElementByClass('save-msg-container',
                                             this.container);
  indeed.foundation.alerts.displayError(container, message);
};
