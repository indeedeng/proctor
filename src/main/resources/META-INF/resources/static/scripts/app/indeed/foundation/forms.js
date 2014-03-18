goog.provide('indeed.foundation.forms');

goog.require('goog.dom');
goog.require('goog.dom.classes');


/**
 * Creates/updates error element and appends it after the input element.
 *
 * @param {Element|Node} el Input element to add error.
 * @param {string} errormessage Error message.
 * @param {Element=} opt_label Optional label element to add error.
 */
indeed.foundation.forms.addError = function(el, errormessage, opt_label) {
  if (opt_label) {
    goog.dom.classes.add(opt_label, 'error');
  }
  var parent = /** @type {Element} */ el.parentNode;
  var small = goog.dom.getElementsByTagNameAndClass(goog.dom.TagName.SMALL,
                                                    'error', parent);
  if (small.length == 0) {
    small = goog.dom.createDom(goog.dom.TagName.SMALL, {'class': 'error'});
    goog.dom.insertSiblingAfter(small, el);
  } else {
    small = small[0];
  }
  small.innerHTML = errormessage;
  goog.dom.classes.add(el, 'error');
};


/**
 *
 * @param {Element|Node} el Input element from which to remove the error.
 * @param {Element=} opt_label Optional label element to remove the error.
 */
indeed.foundation.forms.removeError = function(el, opt_label) {
  var parent = /** @type {Element} */ el.parentNode;
  var small = goog.dom.getElementsByTagNameAndClass(goog.dom.TagName.SMALL,
                                                    'error',
                                                    parent);
  if (small.length > 0) {
    goog.dom.removeNode(small[0]);
  }
  if (opt_label) {
    goog.dom.classes.remove(opt_label, 'error');
  }
  goog.dom.classes.remove(el, 'error');
};
