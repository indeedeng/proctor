goog.provide('indeed.proctor.CenterWindowPosition');

goog.require('goog.math.Box');
goog.require('goog.math.Coordinate');
goog.require('goog.math.Size');
goog.require('goog.positioning');
goog.require('goog.positioning.AbstractPosition');



/**
 * Encapsulates a popup position where position is determined absolutely
 * trying to center
 *
 * @param {Element=} opt_window Element to center within.
 * @constructor
 * @extends {goog.positioning.AbstractPosition}
 */
indeed.proctor.CenterWindowPosition = function(opt_window) {
  /**
   * Element to align centers (if possible).
   * @type {Element|Window}
   */
  this.center_in = opt_window || window;
  this.dom_ = goog.dom.getDomHelper(this.center_in);
};
goog.inherits(indeed.proctor.CenterWindowPosition,
              goog.positioning.AbstractPosition);


/**
 * Repositions the popup according to the current state.
 *
 * @param {Element} movableElement The DOM element to position.
 * @param {goog.positioning.Corner} movableCorner The corner of the movable
 *     element that should be positioned at the specified position.
 * @param {goog.math.Box=} opt_margin A margin specified in pixels.
 * @param {goog.math.Size=} opt_preferredSize Prefered size of the
 *     movableElement.
 */
indeed.proctor.CenterWindowPosition.prototype.reposition = function(
    movableElement, movableCorner, opt_margin, opt_preferredSize) {
  var scroll = this.dom_.getDocumentScroll();
  var x = scroll.x;
  var y = scroll.y;

  var elSize = goog.style.getSize(movableElement);
  var viewSize = this.dom_.getViewportSize();

  // Make sure left and top are non-negatives.
  var left = Math.max(x + viewSize.width / 2 - elSize.width / 2, 0);
  var top = Math.max(y + viewSize.height / 2 - elSize.height / 2, 0);

  var coordinate = new goog.math.Coordinate(left, top);

  goog.positioning.positionAtCoordinate(coordinate,
                                        movableElement,
                                        goog.positioning.Corner.TOP_LEFT,
                                        opt_margin,
                                        null,
                                        null,
                                        opt_preferredSize);
};

