<#if useClosure>
goog.provide('${packageName}');



/**
 * Entry point for javascript to assign test groups and query for
 * group membership when needed.
 *
 * Call init() to create a global instance of ${groupsClassName}_.
 * Call getGroups() to retrieve the reference.
 */
${packageName} = (function() {
<#else>
define('${packageName}', [], function() {
</#if>

  /**
   * Sets proctor groups based on an array of integer values
   * that matches the number of tests in the proctor specification
   * and is in alphabetical order with respect to proctor test names.
   *
   * If no values are supplied, sets default values for all groups.
   *
   * @param {Array.<Object>=} opt_values a list of integers that matches proctor test groups for the request.
   * @private
   * @constructor
   */
  var ${groupsClassName}_ = function(opt_values) {
    if (opt_values) {
      var testDef;
      <#list testDefs as testDef>
      testDef = opt_values[${testDef_index}];
      this.${testDef.normalizedName}Value_ = testDef[0];
      <#if (testDef.payloadJavascriptType)??>
      this.${testDef.normalizedName}PayloadValue_ = testDef[1];
      </#if>
      </#list>
    } else {
      <#list testDefs as testDef>
      this.${testDef.normalizedName}Value_ = ${testDef.defaultValue};
      <#if (testDef.payloadJavascriptType)??>
      this.${testDef.normalizedName}PayloadValue_ = ${testDef.payloadDefaultValue};
      </#if>
      </#list>
    }
  };


  <#list testDefs as testDef>
  // ${testDef.enumName}


  /**
   * Bucket value for ${testDef.normalizedName}.
   * @type {number}
   * @private
   */
  ${groupsClassName}_.prototype.${testDef.normalizedName}Value_;


  <#if (testDef.payloadJavascriptType)??>
  /**
   * Payload value for ${testDef.normalizedName}.
   * @type {${testDef.payloadJavascriptType}}
   * @private
   */
  ${groupsClassName}_.prototype.${testDef.normalizedName}PayloadValue_;


  /**
   * Get the payload value for ${testDef.normalizedName}.
   * @return {${testDef.payloadJavascriptType}}
   */
  ${groupsClassName}_.prototype.get${testDef.javaClassName}PayloadValue = function() {
    return this.${testDef.normalizedName}PayloadValue_;
  };
  </#if>


  <#list testDef.buckets as bucket>
  /**
   * Checks if the user is in ${testDef.normalizedName}${bucket.javaClassName}.
   * @return {boolean}
   */
  ${groupsClassName}_.prototype.is${testDef.javaClassName}${bucket.javaClassName} = function() {
    return this.${testDef.normalizedName}Value_ === ${bucket.value};
  };


  </#list>

  </#list>
  /**
  * Static reference to instance of groups class.
  *
  * @type {${groupsClassName}_}
  * @private
  */
  var groups_ = new ${groupsClassName}_();

  return {

    /**
    * Create an instance of ${groupsClassName}_.
    * Assign it to a static reference and return it.
    *
    * @param {Array.<Object>} values a list of integers that matches proctor test groups for the request.
    * @return {${groupsClassName}_}
    */
    init: function(values) {
      groups_ = new ${groupsClassName}_(values);
      return groups_;
    },


    /**
    * Get the static reference to our groups instance.
    * If it has not been initialized, return a default instance.
    *
    * @return {${groupsClassName}_}
    */
    getGroups: function() {
      return groups_;
    }

  };
<#if useClosure>
}());
<#else>
});
</#if>