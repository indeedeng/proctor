<#if useClosure>
goog.provide('${packageName}');



</#if>
/**
* Sets proctor groups based on an array of integer values
* that matches the number of tests in the proctor specification
* and is in alphabetical order with respect to proctor test names.
*
* @param {Array.<Object>} values a list of integers that matches proctor test groups for the request.
*/
${packageName}.init = function(values) {
    var i = 0;
    <#list testDefs as testDef>
    ${packageName}.${testDef.normalizedName}Value = values[i][0];
    <#if (testDef.payloadJavascriptType)??>
    ${packageName}.${testDef.normalizedName}PayloadValue = values[i][1];
    </#if>
    i++;
    </#list>
};



<#list testDefs as testDef>
// ${testDef.enumName}


/**
* Bucket value for ${testDef.normalizedName}.
* @type {number}
* @private
*/
${packageName}.${testDef.normalizedName}Value;


<#if (testDef.payloadJavascriptType)??>
/**
* Payload value for ${testDef.normalizedName}.
* @type {${testDef.payloadJavascriptType}}
* @private
*/
${packageName}.${testDef.normalizedName}PayloadValue;


/**
* Get the payload value for ${testDef.normalizedName}.
* @return {${testDef.payloadJavascriptType}}
*/
${packageName}.get${testDef.javaClassName}PayloadValue = function() {
    return ${packageName}.${testDef.normalizedName}PayloadValue;
};
</#if>


<#list testDef.buckets as bucket>
/**
* Checks if the user is in ${testDef.normalizedName}${bucket.javaClassName}.
* @return {boolean}
*/
${packageName}.is${testDef.javaClassName}${bucket.javaClassName} = function() {
    return ${packageName}.${testDef.normalizedName}Value == ${bucket.value};
};


</#list>

</#list>
<#if !useClosure>
module.exports = ${packageName};
</#if>