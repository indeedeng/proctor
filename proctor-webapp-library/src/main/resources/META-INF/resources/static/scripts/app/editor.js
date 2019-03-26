goog.provide('indeed.proctor.app.editor');

goog.require('goog.array');
goog.require('goog.dom');
goog.require('goog.events');
goog.require('goog.events.EventHandler');
goog.require('goog.events.EventTarget');
goog.require('goog.json');
goog.require('goog.net.XhrIo');
goog.require('goog.style');
goog.require('goog.uri.utils');
goog.require('indeed.expandcollapse.ExpandCollapse');
goog.require('indeed.foundation.Tabs');
goog.require('indeed.foundation.alerts');
goog.require('indeed.proctor.JobMonitor');
goog.require('indeed.proctor.editor.AllocationsEditor');
goog.require('indeed.proctor.editor.BasicEditor');
goog.require('indeed.proctor.editor.BucketsEditor');
goog.require('indeed.proctor.editor.ConstantsEditor');
goog.require('indeed.proctor.editor.SvnInfoEditor');
goog.require('indeed.proctor.editor.CleanWorkspace');

/**
 * Entry point for editor.jsp
 * @param {string} testName Test Name.
 * @param {Object} definition JSON.
 * @param {string} svnRevision current svn revision.
 * @param {boolean} isCreate Is New Test.
 */
indeed.proctor.app.editor.start =
    function(testName, definition, svnRevision, isCreate) {
  goog.events.listen(window, 'load', function() {

    var editor = new indeed.proctor.app.editor.DefinitionEditor(
        goog.dom.getElementByClass('js-edit-definition-form'),
        testName, definition, svnRevision, isCreate);

    var tabs = goog.dom.getElementsByClass('js-tabs-container');
    goog.array.forEach(tabs, function(tab) {
      var uiTab = new indeed.foundation.Tabs(tab);
    });

    var definitions = goog.dom.getElementsByClass('js-promote-definition');
    goog.array.forEach(definitions, function(promote) {
      var uiPromote = new indeed.proctor.editor.SvnInfoEditor(promote, true);
    });

    indeed.expandcollapse.ExpandCollapse.detect(document.body);
    indeed.proctor.editor.CleanWorkspace.detect(document.body);

  });
};



/**
 *
 * @param {Element} container Root Element.
 * @param {string} testName test name.
 * @param {Object} definition JSON definition.
 * @param {string} svnRevision svn revision.
 * @param {boolean} isCreate Is new test.
 * @constructor
 */
indeed.proctor.app.editor.DefinitionEditor =
    function(container, testName, definition, svnRevision, isCreate) {
  this.container = container;
  this.allocationEditors_ =
      new indeed.proctor.editor.AllocationsEditor(
      goog.dom.getElementByClass('js-allocations-editor'),
      definition, isCreate);
  this.bucketsEditor_ =
      new indeed.proctor.editor.BucketsEditor(
      goog.dom.getElementByClass('js-bucket-editor'),
      definition, isCreate);
  this.basicEditor_ =
      new indeed.proctor.editor.BasicEditor(
      goog.dom.getElementByClass('js-basic-editor'),
      definition, isCreate);
  this.constantsEditors_ = [];
  var els = goog.dom.getElementsByTagNameAndClass(undefined,
                                                  'js-constants-editor',
                                                  container);
  for(var i = 0; i < els.length; i++) {
    this.constantsEditors_.push(new indeed.proctor.editor.ConstantsEditor(
        els[i], definition, isCreate
    ));
  }
  this.isCreate = isCreate;
  this.testName = testName;

  this.prevRevision = svnRevision;

  this.handler_ = new goog.events.EventHandler(this);

  this.handler_.listen(this.bucketsEditor_,
                       ['bucketAdded', 'bucketChanged', 'bucketDeleted'],
                       function(ev) {
        this.allocationEditors_.bucketsUpdated(ev.buckets);
      });

  this.bind_();
};


/**
 * Identify elements within container + add event listeners
 * @private
 */
indeed.proctor.app.editor.DefinitionEditor.prototype.bind_ = function() {
  if (!this.isCreate && this.basicEditor_.silent.value === "true") {
    this.bindWarningForSilent_();
  }
  this.definitionForm = goog.dom.getElementByClass('js-edit-definition-form');
  this.handler_.listen(this.definitionForm,
                       goog.events.EventType.SUBMIT, this.onFormSubmit_);


  var saveForm = goog.dom.getElementsByTagNameAndClass(null,
                                                       'js-save-form',
                                                       this.container);
  for (var i = 0; i < saveForm.length; i++) {
    this.handler_.listen(saveForm[i], goog.events.EventType.CLICK,
                         this.onSaveClick_);
  }

  var prodPromotionCheckbox = goog.dom.getElement('autopromote-prod');
  this.handler_.listen(prodPromotionCheckbox, goog.events.EventType.CLICK, this.onProdPromotionClick_);

  var saveinfo = goog.dom.getElementByClass('js-save-info', this.container);
  this.svninfo = new indeed.proctor.editor.SvnInfoEditor(saveinfo, false);

};

/**
 * add event listener warning when silent is true but test is active
 * @private
 */
indeed.proctor.app.editor.DefinitionEditor.prototype.bindWarningForSilent_ = function () {
  this.handler_.listen(this.allocationEditors_, ['ratioChange', 'ratioAdded'], goog.bind(function () {
    if (this.allocationEditors_.validate() && this.allocationEditors_.checkActive()) {
      this.basicEditor_.silent.checked = false;
      goog.dom.getElement('silent-warning').textContent = "Logging is automatically enabled for this test because you change allocations. If you don't need logging, please check the 'Silent' checkbox at the top of the page.";
    }
  }, this));
};


/**
 *
 * @param {goog.events.BrowserEvent} ev Event Facade/.
 * @private
 */
indeed.proctor.app.editor.DefinitionEditor.prototype.onFormSubmit_ =
    function(ev) {
  ev.preventDefault();
  this.save_();
};


/**
 *
 * @param {goog.events.BrowserEvent} ev Event Facade/.
 * @private
 */
indeed.proctor.app.editor.DefinitionEditor.prototype.onSaveClick_ =
    function(ev) {
  ev.preventDefault();
  this.save_();
};

/**
 *
 * @param ev {goog.events.BrowserEvent} ev Event Facade/.
 * @private
 */
indeed.proctor.app.editor.DefinitionEditor.prototype.onProdPromotionClick_ = function(ev) {
  var prodCheckbox = ev.target;
  var qaCheckbox = goog.dom.getElement("autopromote-qa");

  if (prodCheckbox.checked) {
    qaCheckbox.checked = true;
    qaCheckbox.disabled = true;
  } else {
    qaCheckbox.disabled = false;
  }
};


/**
 * @private
 */
indeed.proctor.app.editor.DefinitionEditor.prototype.save_ = function() {
  var valid = true;
  valid = this.basicEditor_.validate() && valid;
  valid = this.bucketsEditor_.validate() && valid;
  valid = this.allocationEditors_.validate() && valid;
  valid = goog.array.every(this.constantsEditors_, function(v) { return v.validate() } ) && valid;

  if (!valid) {
    this.displayError_('Errors in above sections, not saving.');
    return;
  } else {
    this.hideMessage_();
  }

  valid = this.svninfo.validate() && valid;

  if (!valid) {
    return;
  }

  var jsoninputs = goog.array.concat(
      goog.array.toArray(goog.dom.getElementsByTagNameAndClass(
          goog.dom.TagName.INPUT, 'json', document)),
      goog.array.toArray(goog.dom.getElementsByTagNameAndClass(
          goog.dom.TagName.TEXTAREA, 'json', document)),
      goog.array.toArray(goog.dom.getElementsByTagNameAndClass(
          goog.dom.TagName.SELECT, 'json', document))
      );

  var json = indeed.proctor.forms.toJSON(jsoninputs);

  if (!json['constants']) {
    json['constants'] = {};
  }

  var serializer = new goog.json.Serializer();

  var formElementsArray = document.forms[0].elements;
  var formData = {};
  for (var i = 0; i < formElementsArray.length; i++) {
      formData[formElementsArray[i].name] = formElementsArray[i].value;
  }

  var promoteToQA = goog.dom.getElement("autopromote-qa").checked;
  var promoteToProd = goog.dom.getElement("autopromote-prod").checked;
  var autopromoteTarget = "none";

  if (promoteToProd) {
    autopromoteTarget = "qa-and-prod";
  } else if (promoteToQA) {
    autopromoteTarget = "qa";
  }

  var jsData = {
    'testDefinition': serializer.serialize(json),
    'comment': goog.dom.forms.getValue(this.svninfo.comment),
    'previousRevision': this.prevRevision,
    'isCreate' : this.isCreate,
    'isAutopromote' : promoteToQA || promoteToProd,
    'autopromoteTarget' : autopromoteTarget
  };
  if (this.svninfo.username && this.svninfo.password) {
    jsData['username'] = goog.dom.forms.getValue(this.svninfo.username);
    jsData['password'] = goog.dom.forms.getValue(this.svninfo.password);
  }

  var data = {};
  for (var key in formData) {
      data[key] = formData[key];
  }
  for (var key in jsData) {
      data[key] = jsData[key];
  }

  this.displayProgress_('Saving ... ');
  var url = this.definitionForm.action;
  if (this.isCreate) {
    url = url.replace(encodeURIComponent('{testName}'),
                      this.basicEditor_.getTestName());
  }
  goog.net.XhrIo.send(
      url,
      goog.bind(this.onSaveCallback_, this),
      'POST',
      goog.uri.utils.buildQueryDataFromMap(data),
      {'X-Requested-With': 'XMLHttpRequest'},
      /* no timeout */ 0
  );
};


/**
 *
 * @param {goog.events.Event} e Event Facade.
 * @private
 */
indeed.proctor.app.editor.DefinitionEditor.prototype.onSaveCallback_ =
    function(e) {
  var xhr = /** goog.net.XhrIo */ e.target, job;
  if (xhr.isSuccess()) {
    var json = xhr.getResponseJson();
    if (json['success']) {

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
indeed.proctor.app.editor.DefinitionEditor.prototype.displayProgress_ =
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
indeed.proctor.app.editor.DefinitionEditor.prototype.displaySuccess_ =
    function(message) {
  var container = goog.dom.getElementByClass('save-msg-container',
                                             this.container);
  indeed.foundation.alerts.displaySuccess(container, message);
};


/**
 *
 * @private
 */
indeed.proctor.app.editor.DefinitionEditor.prototype.hideMessage_ =
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
indeed.proctor.app.editor.DefinitionEditor.prototype.displayError_ =
    function(message) {
  var container = goog.dom.getElementByClass('save-msg-container',
                                             this.container);
  indeed.foundation.alerts.displayError(container, message);
};

goog.exportSymbol('indeed.proctor.app.editor.start',
                  indeed.proctor.app.editor.start);

