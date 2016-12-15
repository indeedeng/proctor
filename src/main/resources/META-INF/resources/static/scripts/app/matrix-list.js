goog.provide('indeed.proctor.app.jobs.start');
goog.provide('indeed.proctor.app.matrix.list');
goog.provide('indeed.proctor.app.matrix.usage');

goog.require('goog.array');
goog.require('goog.dom');
goog.require('goog.events');
goog.require('goog.events.EventHandler');
goog.require('goog.events.EventTarget');
goog.require('goog.events.EventType');
goog.require('indeed.expandcollapse.ExpandCollapse');
goog.require('indeed.foundation.Tabs');
goog.require('indeed.proctor.JobMonitor');
goog.require('indeed.proctor.editor.AllocationsEditor');
goog.require('indeed.proctor.editor.CleanWorkspace');
goog.require('indeed.proctor.filter.Filter');
goog.require('indeed.proctor.filter.Sorter');
goog.require('indeed.proctor.filter.Favorites');


/**
 * entry point for the matrix.list
 */
indeed.proctor.app.matrix.list.start = function(matrix) {
  goog.events.listen(window, 'load', function() {

    indeed.expandcollapse.ExpandCollapse.detect(document.body);


    var tabs = goog.dom.getElementsByClass('js-tabs-container');
    goog.array.forEach(tabs, function(tab) {
      var uiTab = new indeed.foundation.Tabs(tab);
    });
    var filterContainer = goog.dom.getElement("filter-container");
    var testContainer = goog.dom.getElement("test-container");
    new indeed.proctor.filter.Filter(matrix, filterContainer);
    new indeed.proctor.filter.Favorites(testContainer);
    new indeed.proctor.filter.Sorter(filterContainer, testContainer);
  });
};


/**
 * entry point for the matrix.usage
 */
indeed.proctor.app.matrix.usage.start = function() {
  goog.events.listen(window, 'load', function() {

    indeed.expandcollapse.ExpandCollapse.detect(document.body);

    var tabs = goog.dom.getElementsByClass('js-tabs-container');
    goog.array.forEach(tabs, function(tab) {
      var uiTab = new indeed.foundation.Tabs(tab);
    });
  });
};


/**
 * entry point for the jobs.list
 */
indeed.proctor.app.jobs.start = function() {
  goog.events.listen(window, 'load', function() {
    indeed.expandcollapse.ExpandCollapse.detect(document.body);
    var anchors =
        goog.dom.getElementsByTagNameAndClass(goog.dom.TagName.A,
                                              'js-job-view'),
        querystring = window.location.search.substring(1),
        queryparams = indeed.proctor.app.matrix.buildFromString(querystring);
    var load_ = function(ev) {
      var el = ev.currentTarget,
          href = el.href,
          jobid = href.substring(href.indexOf('#') + 1);
      ev.preventDefault();
      indeed.proctor.JobMonitor.showMonitorAsPopup(jobid, true);
    };
    goog.array.forEach(anchors, function(el) {
      goog.events.listen(el, goog.events.EventType.CLICK, load_);
    });
    if(queryparams.get('id')) {
      indeed.proctor.JobMonitor.showMonitorAsPopup(queryparams.get('id'), true);
    }
    indeed.proctor.editor.CleanWorkspace.detect(document.body);
  });
};


/**
 * Builds a Params object from an encoded string of params
 * @param {string} params Param string.
 * @return {!indeed.common.util.Params} A Params object representing the param
 * string.
 */
indeed.proctor.app.matrix.buildFromString = function(params) {
    var paramList = params.split('&');

    var paramObject = new indeed.common.util.Params();

    for (var i = 0; i < paramList.length; i++) {
        var param = paramList[i];
        var equalsIndex = param.indexOf('=');
        var key = '';
        var value = '';

        if (equalsIndex > 0) {
            key = param.substring(0, equalsIndex);
            value = param.substring(equalsIndex + 1);
        }
        else if (equalsIndex < 0) {
            key = param;
        }

        if (key) {
            value = indeed.proctor.app.matrix.decodeURIComponent(value);
            paramObject.set(key, value);
        }
    }

    return paramObject;
};

/**
 * Decodes a URI component and converts + back to spaces
 * @param {string} uriComponent component to convert.
 * @return {string} Decoded component.
 */
indeed.proctor.app.matrix.decodeURIComponent = function(uriComponent) {
    return decodeURIComponent(uriComponent.replace(/\+/g, ' '));
};


goog.exportSymbol('indeed.proctor.app.matrix.list.start',
                  indeed.proctor.app.matrix.list.start);
goog.exportSymbol('indeed.proctor.app.matrix.usage.start',
                  indeed.proctor.app.matrix.usage.start);
goog.exportSymbol('indeed.proctor.app.jobs.start',
                  indeed.proctor.app.jobs.start);

