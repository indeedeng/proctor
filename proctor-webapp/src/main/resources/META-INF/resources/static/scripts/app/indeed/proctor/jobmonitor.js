goog.provide('indeed.proctor.JobMonitor');
goog.provide('indeed.proctor.JobMonitor.Event');
goog.provide('indeed.proctor.JobMonitor.EventTypes');



goog.require('goog.dom');
goog.require('goog.dom.forms');
goog.require('goog.events');
goog.require('goog.events.EventHandler');
goog.require('goog.events.EventTarget');
goog.require('goog.json');
goog.require('goog.net.XhrIo');
goog.require('goog.ui.Popup');
goog.require('indeed.proctor.CenterWindowPosition');



/**
 * @param {Element} container Root element.
 * @param {string} jobid Element Container.
 *
 * @constructor
 * @extends {goog.events.EventTarget}
 */
indeed.proctor.JobMonitor = function(container, jobid) {
  indeed.proctor.JobMonitor.base(this, 'constructor');
  /** @type {string} */
  this.jobid = jobid;
  /** @type {number} */
  this.refresh_interval = 1000;
  /** @type {number} */
  this.max_retries = 8;
  /** @type {number} */
  this.time_out_ms = 10000;

  this.retries_ = 0;

  /** @type {goog.net.XhrIo} @private */
  this.xhr_ = new goog.net.XhrIo();
  this.xhr_.setTimeoutInterval(this.time_out_ms);

  goog.events.listen(this.xhr_, goog.net.EventType.SUCCESS,
                     this.onAjaxSuccess_, false, this);
  goog.events.listen(this.xhr_, goog.net.EventType.TIMEOUT,
                     this.onAjaxError_, false, this);
  goog.events.listen(this.xhr_, goog.net.EventType.ERROR,
                     this.onAjaxError_, false, this);

  /** @type {Element} @private */
  this.boundingBox_ = this.render_(container);

  this.checkStatus();
  this.checkStatusTimerId_ = null;
};
goog.inherits(indeed.proctor.JobMonitor, goog.events.EventTarget);


/**
 * @param {Element} root The root element to render into.
 * @private
 * @return {Element} The bounding box.
 */
indeed.proctor.JobMonitor.prototype.render_ = function(root) {
  var boundBox = goog.dom.createDom(goog.dom.TagName.DIV,
                                    'ui-job-monitor-bounds'),
      content = goog.dom.createDom(goog.dom.TagName.DIV,
                                   'ui-job-monitor-content'),
      title = goog.dom.createDom(goog.dom.TagName.H5,
                                 'ui-job-monitor-title'),
      status = goog.dom.createDom(goog.dom.TagName.H6,
                                  'ui-job-monitor-status'),
      cancel = goog.dom.createDom(goog.dom.TagName.A,
                    'ui-job-monitor-cancel mbs tiny button radius secondary'),
      textarea = goog.dom.createDom(goog.dom.TagName.TEXTAREA,
                                    'ui-job-monitor-log');


  goog.dom.setTextContent(cancel, 'cancel');
  cancel.href = '#';

  goog.dom.appendChild(boundBox, content);
  goog.dom.appendChild(content, title);
  goog.dom.appendChild(content, status);
  goog.dom.appendChild(content, textarea);
  goog.dom.appendChild(content, cancel);

  /** @type {Element} */
  this.title_el = title;
  /** @type {Element} */
  this.status_el = status;

  /** @type {Element} */
  this.log_el = textarea;

  goog.dom.appendChild(root, boundBox);

  goog.events.listen(cancel, goog.events.EventType.CLICK, function(ev) {
    ev.preventDefault();
    this.cancelJob();
  }, false, this);

  goog.events.listen(this, [indeed.proctor.JobMonitor.EventTypes.COMPLETE,
    indeed.proctor.JobMonitor.EventTypes.CANCELLED,
    indeed.proctor.JobMonitor.EventTypes.ERROR],
  this.onJobFinished_, false, this);
  return boundBox;
};


/**
 *
 * @param {indeed.proctor.JobMonitor.Event} ev The event.
 * @private
 */
indeed.proctor.JobMonitor.prototype.onJobFinished_ = function(ev) {
  var cancel = goog.dom.getElementByClass('ui-job-monitor-cancel',
                                          this.boundingBox_),
      status = goog.dom.getElementByClass('ui-job-monitor-log',
                                          this.boundingBox_),
      job = ev.job_status,
      urls = job['urls'],
      endMessage = job['endMessage'],
      ul;
  if (cancel) {
    goog.events.removeAll(cancel);
    goog.dom.removeNode(cancel);
  }

  ul = goog.dom.createDom(goog.dom.TagName.UL, 'link-list');
  if (endMessage) {
    var li = goog.dom.createDom(goog.dom.TagName.LI, undefined, endMessage);
    goog.dom.appendChild(ul, li);
  }
  if (goog.isArray(urls)) {
    goog.array.forEach(urls, function(url) {
      var li = goog.dom.createDom(goog.dom.TagName.LI);
      var link = goog.dom.createDom(goog.dom.TagName.A, {
                             'class': 'button small secondary',
                             'target': url['target'],
                             'href': url['href']
                           }, url['text']);
      goog.dom.appendChild(li, link);
      goog.dom.appendChild(ul, li);
    });
  }
  if (ul.hasChildNodes()) {
    goog.dom.insertSiblingAfter(ul, status);
  }
};


/**
 * Updates the contents of the JobMonitor UI
 * @param {string} title Title to display.
 * @param {string} status Status to display.
 * @param {string} log Log message to display.
 */
indeed.proctor.JobMonitor.prototype.update = function(title, status, log) {
  goog.dom.setTextContent(this.title_el, title);
  goog.dom.setTextContent(this.status_el, 'Status: ' + status);
  goog.dom.setTextContent(this.log_el, log);
};


/**
 * Checks the status of the job, sending an xhr request to the webapp
 */
indeed.proctor.JobMonitor.prototype.checkStatus = function() {
  var url;
  if (this.xhr_.isActive()) {
    this.xhr_.abort();
  }
  if (this.checkStatusTimerId_) {
    window.clearTimeout(this.checkStatusTimerId_);
    this.checkStatusTimerId_ = null;
  }
  url = goog.uri.utils.appendParams('/proctor/rpc/jobs/status', 'id',
                                    this.jobid);
  this.xhr_.send(url,
                 'GET',
                 undefined,
                 {'X-Requested-With': 'XMLHttpRequest'}
  );
};


/**
 * Cancel the job, sending an xhr request to the webapp.
 */
indeed.proctor.JobMonitor.prototype.cancelJob = function() {
  var url;
  if (this.xhr_.isActive()) {
    this.xhr_.abort();
  }
  if (this.checkStatusTimerId_) {
    window.clearTimeout(this.checkStatusTimerId_);
    this.checkStatusTimerId_ = null;
  }
  url = goog.uri.utils.appendParams('/proctor/rpc/jobs/cancel', 'id',
                                    this.jobid);
  this.xhr_.send(url,
                 'GET',
                 undefined,
                 {'X-Requested-With': 'XMLHttpRequest'}
  );
};


/**
 *
 * @param {goog.events.Event} ev XHR event, on Ajax Success.
 * Handle the json content and dispatch the appropriate JobMonitor event.
 * @private
 */
indeed.proctor.JobMonitor.prototype.onAjaxSuccess_ = function(ev) {
  var xhr = ev.target,
      resp = xhr.getResponseJson(), job;
  if (resp['success']) {
    job = resp['data'];
    this.update(job['title'],
                job['running'] ? ('RUNNING') : job['status'],
                job['log']);
    if (job['running']) {
      this.dispatchEvent(new indeed.proctor.JobMonitor.Event(
          indeed.proctor.JobMonitor.EventTypes.IN_PROGRESS, this.jobid, job));
      this.checkStatusTimerId_ = window.setTimeout(
          goog.bind(this.checkStatus, this), this.refresh_interval);
    } else if ('DONE' == job['status'] || 'FAILED' == job['status']) {
      this.dispatchEvent(new indeed.proctor.JobMonitor.Event(
          indeed.proctor.JobMonitor.EventTypes.COMPLETE, this.jobid, job));
    } else if ('CANCELLED' == job['status']) {
      this.dispatchEvent(new indeed.proctor.JobMonitor.Event(
          indeed.proctor.JobMonitor.EventTypes.CANCELLED, this.jobid, job));
    }
  } else {
    this.update(resp['msg'], 'ERROR', resp['msg']);
    this.dispatchEvent(new indeed.proctor.JobMonitor.Event(
        indeed.proctor.JobMonitor.EventTypes.ERROR, this.jobid));
  }
};


/**
 *
 * @param {goog.events.Event} ev XHR event, on Ajax Error.
 * @private
 */
indeed.proctor.JobMonitor.prototype.onAjaxError_ = function(ev) {
  this.retries_ = this.retries_ + 1;
  if (this.retries_ > this.max_retries) {
    /** @type {goog.net.XhrIo} */
    var xhr = ev.target;
    var msg = xhr.getLastErrorCode() + ': ' + xhr.getLastError();
    this.update(msg,
      'ERROR',
      ['Failed to retrieve job status for ' + this.jobid, msg].join('\n')
    );
    if (this.xhr_.isActive()) {
      this.xhr_.abort();
    }
    this.dispatchEvent(new indeed.proctor.JobMonitor.Event(
      indeed.proctor.JobMonitor.EventTypes.ERROR, this.jobid));
  } else {
    this.checkStatusTimerId_ = window.setTimeout(
      goog.bind(this.checkStatus, this), this.refresh_interval);
  }
};


/**
 * Event types for indeed.proctor.JobMonitor.
 * @enum {string}
 */
indeed.proctor.JobMonitor.EventTypes = {
  COMPLETE: 'complete',
  CANCELLED: 'cancelled',
  IN_PROGRESS: 'in-progress',
  ERROR: 'error'
};


/** @override */
indeed.proctor.JobMonitor.prototype.disposeInternal = function() {
  indeed.proctor.JobMonitor.base(this, 'disposeInternal');
  if (this.checkStatusTimerId_) {
    window.clearTimeout(this.checkStatusTimerId_);
  }
  goog.dispose(this.xhr_);
  delete this.xhr_;
};



/**
 * Event object dispatched after the history state has changed.
 * @param {indeed.proctor.JobMonitor.EventTypes} type The type
 * of this event.
 * @param {string} jobid The job id for this event.
 * @param {Object=} opt_job_status The job status returned from the rpc
 * controller. Null if type==error.
 * @constructor
 * @extends {goog.events.Event}
 */
indeed.proctor.JobMonitor.Event = function(type, jobid, opt_job_status) {
  goog.events.Event.call(this, type);
  /** @type {string} */
  this.jobid = jobid;

  /** @type {?Object|undefined} */
  this.job_status = opt_job_status;
};
goog.inherits(indeed.proctor.JobMonitor.Event, goog.events.Event);


/**
 *
 * @param {string} jobid The job id.
 * @param {boolean=} opt_allow_close Optional boolean, default false, indicating
 * if the user should be able to close the popup with escape / clicking off.
 * @return {indeed.proctor.JobMonitor} The job monitor instance created.
 */
indeed.proctor.JobMonitor.showMonitorAsPopup =
    function(jobid, opt_allow_close) {
  var container = goog.dom.createDom(goog.dom.TagName.DIV,
                                     'ui-job-monitor-popup panel radius'),
      jm = new indeed.proctor.JobMonitor(container, jobid),
      popup = new goog.ui.Popup(container,
                                new indeed.proctor.CenterWindowPosition()),
      allow_close = !!opt_allow_close;

  // append footer
  goog.dom.appendChild(container,
                       goog.dom.createDom(goog.dom.TagName.DIV,
                                          'ft ui-job-monitor-popup-ft'));
  goog.dom.appendChild(document.body, container);

  goog.events.listen(popup,
                     goog.ui.PopupBase.EventType.BEFORE_SHOW,
                     indeed.proctor.JobMonitor.onPopupShow_);

  goog.events.listen(jm,
                     [indeed.proctor.JobMonitor.EventTypes.COMPLETE,
                      indeed.proctor.JobMonitor.EventTypes.CANCELLED,
                       indeed.proctor.JobMonitor.EventTypes.ERROR],
                     indeed.proctor.JobMonitor.onMonitorFinished_,
                     false,
                     popup);


  // popup.render();
  popup.setAutoHide(allow_close);
  popup.setHideOnEscape(allow_close);
  popup.setVisible(true);
  popup.reposition();

  container.style.position = 'absolute';
  goog.events.listen(popup, goog.ui.PopupBase.EventType.HIDE,
                     indeed.proctor.JobMonitor.onHideDestroy(jm));
  return jm;
};


/**
 *
 * @private
 * @return {Element} Gets or creates a popup mask for shared use.
 */
indeed.proctor.JobMonitor.getOrCreateMask_ = function() {
  var mask = goog.dom.getElementByClass('ui-job-monitor-popup-bg');
  if (!mask) {
    mask = goog.dom.createDom(goog.dom.TagName.DIV, 'ui-job-monitor-popup-bg');
    goog.dom.appendChild(document.body, mask);
  }
  return mask;
};


/**
 * Shows a 'close' element in the footer of the popup after the monitor has
 * finished.
 * @param {indeed.proctor.JobMonitor.Event} ev The finished event.
 * @this {goog.ui.Popup}
 * @private
 */
indeed.proctor.JobMonitor.onMonitorFinished_ = function(ev) {
  var popup = this,
      ft = goog.dom.getElementByClass('ft', popup.getElement()),
      hide = goog.dom.createDom(goog.dom.TagName.A, 'button radius medium');

  hide.href = '#hide';
  goog.dom.setTextContent(hide, 'close');
  goog.events.listen(hide, goog.events.EventType.CLICK, function(ev) {
    ev.preventDefault();
    popup.setVisible(false);
  });
  goog.dom.appendChild(ft, hide);
};


/**
 * On show of the popup, show the background mask
 * @private
 */
indeed.proctor.JobMonitor.onPopupShow_ = function() {
  var mask = indeed.proctor.JobMonitor.getOrCreateMask_();
  if (mask) {
    goog.style.showElement(mask, true);
  }
};


/**
 *
 * @param {indeed.proctor.JobMonitor} jm The job monitor.
 * @return {Function} Returns a function with closure to the job monitor
 * that will destroy the job monitor and the popup when the popup is hidden.
 */
indeed.proctor.JobMonitor.onHideDestroy = function(jm) {
  return function(ev) {
    var mask = indeed.proctor.JobMonitor.getOrCreateMask_(),
        popup = ev.currentTarget,
        popup_el = popup.getElement();
    jm.dispose();
    popup.dispose();
    goog.dom.removeNode(popup_el);

    if (mask) {
      goog.style.showElement(mask, false);
    }
  }
};
