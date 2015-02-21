<#if useClosure>
goog.provide('${packageName}');



</#if>
/**
* Sets proctor groups based on an array of integer values
* that matches the number of tests in the proctor specification
* and is in alphabetical order with respect to proctor test names.
*
* @param {array} values a list of integers that matches proctor test groups for the request.
*/
${packageName}.init = function(values) {
    var i = 0;
    <#list testDefs as testDef>
    ${packageName}.${testDef.normalizedName}Value = values[i++];
    </#list>
};



<#list testDefs as testDef>
// ${testDef.enumName}


/**
* Bucket value for ${testDef.normalizedName}.
* @type {int}
*/
${packageName}.${testDef.normalizedName}Value;


<#list testDef.buckets as bucket>
/**
* Checks if the user is in ${testDef.normalizedName}${bucket.javaClassName}.
* @return {boolean}
*/
${packageName}.is${testDef.javaClassName}${bucket.javaClassName} = function () {
    return ${packageName}.${testDef.normalizedName}Value == ${bucket.value};
};


</#list>

</#list>
<#if !useClosure>
module.exports = ${packageName};
</#if>