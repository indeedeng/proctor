goog.provide('indeed.proctor.editor.CleanWorkspace');

goog.require('goog.json');
goog.require('goog.net.XhrIo');
goog.require('goog.array');
goog.require('goog.dom');
goog.require('goog.events');
goog.require('goog.events.EventTarget');
goog.require('goog.uri.utils');
goog.require('indeed.proctor.JobMonitor');

/**
 * Static utility method to detect and initialize CleanWorkspace links via ajax
 *
 * @param {Element} root Element under which to search for clean-workspace links
 * @return {Array.<Element>} Array of Elements
 */
indeed.proctor.editor.CleanWorkspace.detect = function(root) {
  var els = goog.dom.getElementsByTagNameAndClass(null,
                                                  'js-clean-workspace', root);
  var cleanLinks = [];
  goog.array.forEach(els, function(el) {
    goog.events.listen(el, goog.events.EventType.CLICK,
                       indeed.proctor.editor.CleanWorkspace.onWorkspaceClick_);
    cleanLinks.push(el);
  });
  return cleanLinks;
};

/**
 * Static utility method to detect and initialize expand/collapse widgets.
 *
 * @param {goog.events.BrowserEvent} ev Browser click event
 * @private
 */
indeed.proctor.editor.CleanWorkspace.onWorkspaceClick_ = function(ev) {
  var el = ev.currentTarget,
      form = goog.dom.getAncestorByTagNameAndClass(el, goog.dom.TagName.FORM),
      usernameInput, username;
  if(form) {
    usernameInput = indeed.proctor.forms.getElementByInputName('username', goog.dom.TagName.INPUT, undefined, form);
    if(indeed.proctor.forms.validateRequired(usernameInput)) {
      username = goog.dom.forms.getValue(usernameInput);

      goog.net.XhrIo.send(
            '/proctor/rpc/svn/clean-working-directory',
            indeed.proctor.editor.CleanWorkspace.onCleanCallback_,
            'POST',
            goog.uri.utils.buildQueryDataFromMap({'username': username}),
            {'X-Requested-With': 'XMLHttpRequest'},
            /* no timeout */ 0
      );
    }
    ev.preventDefault();
  }
};


/**
 *
 * @param {goog.events.Event} e Event Facade.
 * @private
 */
indeed.proctor.editor.CleanWorkspace.onCleanCallback_ = function(ev) {
  var xhr = /** goog.net.XhrIo */ ev.target, job;
  if (xhr.isSuccess()) {
    var json = xhr.getResponseJson();
    if (json['success']) {

      job = json['data'];
      if (job && job['jobId']) {
        indeed.proctor.JobMonitor.showMonitorAsPopup(job['jobId']);
        return;
      }
    }
  }
  window.alert('Unable to detect background job');
};
