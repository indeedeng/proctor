goog.provide('indeed.foundation.Tabs');

goog.require('goog.History');
goog.require('goog.array');
goog.require('goog.events');
goog.require('goog.events.EventHandler');
goog.require('goog.events.EventTarget');
goog.require('goog.events.EventType');



/**
 *
 * expected dom structure:
 *  <div id="my_tab_container">
 *    <dl class="tabs">
 *      <dd class="active">
 *        <a href="#{tab.id}">Tab Name</a>
 *    <ul class="tabs-content">
 *       <li class="active" id="{tab.id}"
 *          [[tab-content]]
 * @param {Element} el Root element that the tabs should be under.
 * @constructor
 * @extends {goog.events.EventTarget}
 */
indeed.foundation.Tabs = function(el) {
  indeed.foundation.Tabs.base(this, 'constructor');
  /**
   * @type {Element} Container element
   */
  this.container = el;

  /**
   * @type {Array.<{id:string, content:Element, header:Element}>} Tabs
   * @private
   */
  this.tabs_ = [];

  /**
   * @type {Object} Map {string, number} from tab id to index
   */
  this.idToIndex = {};

  /**
   * @type {goog.events.EventHandler} Event Handler for events
   * @private
   */
  this.handler_ = new goog.events.EventHandler(this);

  // select by index, select by id
  this.buildTabsArray_();

  var hash = document.location.hash;
  if (hash.length > 0 && hash.indexOf('#') >= 0) {
    this.selectTabById(hash.substring(hash.indexOf('#') + 1));
  }

  if (goog.History.HAS_ONHASHCHANGE) {
    this.handler_.listen(window, goog.events.EventType.HASHCHANGE,
                         this.onHashChange_);
  }

};
goog.inherits(indeed.foundation.Tabs, goog.events.EventTarget);


/**
 * Event handler when clicking on the tab header.
 * @param {goog.events.BrowserEvent} e Event facade.
 */
indeed.foundation.Tabs.prototype.onTabHeaderClick = function(e) {
  if (!goog.History.HAS_ONHASHCHANGE) {
    e.preventDefault();
  }
  /** @type {Element} */
  var dd = e.currentTarget;
  if (dd.tagName.toUpperCase() !== goog.dom.TagName.DD) {
    /** Element */ dd = goog.dom.getAncestorByTagNameAndClass(dd, goog.dom.TagName.DD);
  }
  var a = goog.dom.getElementsByTagNameAndClass(goog.dom.TagName.A,
                                                null, dd)[0];
  var tabid = this.getTabIdFromHref_(a.href);
  this.selectTabById(tabid);
};


/**
 * Identify the tab structure and populate the tabs_ array.
 * @private
 */
indeed.foundation.Tabs.prototype.buildTabsArray_ = function() {
  var dls = goog.dom.getElementsByTagNameAndClass(goog.dom.TagName.DL,
                                                  'tabs', this.container);
  // assert dls.length == 1
  if (dls.length === 1) {
    var headers = goog.dom.getElementsByTagNameAndClass(goog.dom.TagName.DD,
                                                        null, dls[0]);
    goog.array.forEach(headers, this.addSingleTab_, this);
  }
};


/**
 * Adds a new entry into the tabs_ array at the specified index using the given
 * dd (tab header) element.
 * @param {Element} elDD dd Element.
 * @param {number} index Index into the 'tabs' array.
 * @param {Array} arr Array from for each.
 * @private
 */
indeed.foundation.Tabs.prototype.addSingleTab_ = function(elDD, index, arr) {
  this.handler_.listen(elDD,
                       goog.events.EventType.CLICK, this.onTabHeaderClick);
  var a = goog.dom.getElementsByTagNameAndClass(goog.dom.TagName.A, a, elDD)[0];
  var id = this.getTabIdFromHref_(a.href);
  var content = goog.dom.getElement(id);
  // assert content.tagName == 'li' and content != null

  if (this.tabs_.length > index) {
    goog.array.removeAt(this.tabs_, index); // remove prior to inserting
  }
  goog.array.insertAt(this.tabs_, {
    id: id, header: elDD, content: content
  }, index);

  this.idToIndex[id] = index;
};


/**
 * Selects the specificed tab by the given id.
 * @param {string} id Tab Id to select.
 */
indeed.foundation.Tabs.prototype.selectTabById = function(id) {
  var index = this.idToIndex[id];
  if (typeof index === 'number') {
    this.selectTabByIndex(index);
  }
};


/**
 * Selects the specificed tab by the given index.
 * @param {number} index Tab index to select.
 */
indeed.foundation.Tabs.prototype.selectTabByIndex = function(index) {
  for (var i = 0; i < this.tabs_.length; i++) {
    var isActive = i === index;
    goog.dom.classes.enable(this.tabs_[i].header, 'active', isActive);
    goog.dom.classes.enable(this.tabs_[i].content, 'active', isActive);
  }
  // emit tab selected event
};


/**
 * Parses the tab-id from the tab-header href.
 * @param {string} href The href of the tab header.
 * @private
 * @return {string} The tab id (basically substring after #.
 */
indeed.foundation.Tabs.prototype.getTabIdFromHref_ = function(href) {
  return href.substring(href.lastIndexOf('#') + 1);
};


/**
 * @param {goog.history.Event} ev History event.
 * @private
 */
indeed.foundation.Tabs.prototype.onHashChange_ = function(ev) {
  if (ev.newURL) {
    var tabid = this.getTabIdFromHref_(ev.newURL);
    this.selectTabById(tabid);
  }
};
