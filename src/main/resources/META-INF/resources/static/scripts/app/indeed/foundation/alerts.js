goog.provide('indeed.foundation.alerts');

goog.require('goog.dom');
goog.require('goog.dom.classes');
goog.require('goog.style');
goog.require('goog.string');

/**
 *
 * @param {Element} container Container to set/add message.
 * @param {string} message Message string.
 */
indeed.foundation.alerts.displayProgress = function(container, message) {
  if (container) {
    goog.dom.classes.addRemove(container, ['alert', 'success'], null);
    container.innerHTML = goog.string.htmlEscape(message, false);
    goog.style.showElement(container, true);
  }
};


/**
 *
 * @param {Element} container Container to set/add message.
 * @param {string} message Message string.
 */
indeed.foundation.alerts.displaySuccess = function(container, message) {
  if (container) {
    goog.dom.classes.addRemove(container, 'alert', 'success');
    if (message) {
      container.innerHTML = goog.string.htmlEscape(message, false);
      goog.style.showElement(container, true);
    } else {
      goog.style.showElement(container, false);
    }
  }
};


/**
 *
 * @param {Element} container Container to set/add message.
 * @param {string} message Message string.
 */
indeed.foundation.alerts.displayError = function(container, message) {
  if (container) {
    goog.dom.classes.addRemove(container, 'success', 'alert');
    container.innerHTML = goog.string.htmlEscape(message, false);
    goog.style.showElement(container, true);
  }
};
