goog.provide('indeed.proctor.app.details');

goog.require('goog.array');
goog.require('goog.dom');
goog.require('indeed.expandcollapse.ExpandCollapse');
goog.require('indeed.foundation.Tabs');
goog.require('indeed.proctor.JobMonitor');
goog.require('indeed.proctor.editor.SvnInfoEditor');
goog.require('indeed.proctor.editor.CleanWorkspace');


/**
 * Entry point of the details.jsp page.
 */
indeed.proctor.app.details.start = function() {
  goog.events.listen(window, 'load', function() {
    indeed.expandcollapse.ExpandCollapse.detect(document.body);

    var tabs = goog.dom.getElementsByClass('js-tabs-container');
    goog.array.forEach(tabs, function(tab) {
      var uiTab = new indeed.foundation.Tabs(tab);
    });

    var definitions = goog.dom.getElementsByClass('js-promote-definition');
    goog.array.forEach(definitions, function(promote) {
      var uiPromote = new indeed.proctor.editor.SvnInfoEditor(promote, true);
    });

    var deletions = goog.dom.getElementsByClass('js-delete-definition');
    goog.array.forEach(deletions, function(el) {
      var uiDelete = new indeed.proctor.editor.SvnInfoEditor(el, true);
    });
    indeed.proctor.editor.CleanWorkspace.detect(document.body);

  });
};

goog.exportSymbol('indeed.proctor.app.details.start',
                  indeed.proctor.app.details.start);

