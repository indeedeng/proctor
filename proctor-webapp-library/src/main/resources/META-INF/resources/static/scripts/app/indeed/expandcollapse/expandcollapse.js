goog.provide('indeed.expandcollapse.ExpandCollapse');

goog.require('goog.array');
goog.require('goog.dom');
goog.require('goog.dom.classes');
goog.require('goog.events');
goog.require('goog.events.EventTarget');



/**
 * @param {Element} el Root element of the expand/collapse.
 * @constructor
 */
indeed.expandcollapse.ExpandCollapse = function(el) {
  goog.dom.classes.add(el, 'ui-expand-collapse');

  this.el = el;
  this.collapsed = goog.dom.classes.has(el, 'ui-collapsed');

  this.bind_();
};


/**
 * Identify elements within container + add event listeners
 * @private
 */
indeed.expandcollapse.ExpandCollapse.prototype.bind_ = function() {
  var exCollapseLinks =
      goog.dom.getElementsByTagNameAndClass(null, 'ui-expand-title', this.el);
  for (var i = 0, len = exCollapseLinks.length; i < len; i++) {
    goog.events.listen(exCollapseLinks[i],
                       goog.events.EventType.CLICK,
                       this.onExpandCollapseClick_, false, this);
  }
};


/**
 * Click listener when clicking on ui-expand-title elements.
 * An element can have either / both ui-expand / ui-collapse and will allow the
 * section to be expanded/collapsed as necessary.
 * @param {goog.events.BrowserEvent} ev Event facade.
 * @private
 */
indeed.expandcollapse.ExpandCollapse.prototype.onExpandCollapseClick_ =
    function(ev) {
  var clicked = ev.currentTarget;
  if (this.collapsed && clicked) {
    if (goog.dom.classes.has(clicked, 'ui-expand')) {
      this.expand();
      ev.preventDefault();
    }
  } else if (clicked) {
    if (goog.dom.classes.has(clicked, 'ui-collapse')) {
      this.collapse();
      ev.preventDefault();
    }
  }
};


/**
 * Expand the section.
 */
indeed.expandcollapse.ExpandCollapse.prototype.expand = function() {
  if (this.collapsed) {
    goog.dom.classes.addRemove(this.el, 'ui-collapsed', 'ui-expanded');
    this.collapsed = false;
  }
};


/**
 * Collapse the section.
 */
indeed.expandcollapse.ExpandCollapse.prototype.collapse = function() {
  if (!this.collapsed) {
    goog.dom.classes.addRemove(this.el, 'ui-expanded', 'ui-collapsed');
    this.collapsed = true;
  }
};


/**
 * Static utility method to detect and initialize expand/collapse widgets.
 *
 * @param {Element} root Element under which to search for expand-collapse
 * elements.
 * @return {Array.<indeed.expandcollapse.ExpandCollapse>} Array of initialized
 * expand-collapse widgets.
 */
indeed.expandcollapse.ExpandCollapse.detect = function(root) {
  var els = goog.dom.getElementsByTagNameAndClass(null,
                                                  'ui-expand-collapse', root);
  var expandCollapses = [];
  goog.array.forEach(els, function(el) {
    expandCollapses.push(new indeed.expandcollapse.ExpandCollapse(el));
  });
  return expandCollapses;
};
