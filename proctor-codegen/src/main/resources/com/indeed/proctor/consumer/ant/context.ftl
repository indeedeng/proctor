package ${packageName};

import com.indeed.proctor.common.Identifiers;
import com.indeed.proctor.common.ProctorResult;

import javax.annotation.Nonnull;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;

/*
 * GENERATED source; do not edit directly
 */
public class ${mainClassName} {
<#list contextArguments?keys as contextArgumentName>
    protected final ${contextArguments[contextArgumentName]?replace('$', '.')} ${contextArgumentName};
</#list>

    public ${mainClassName}(final boolean allowForceGroups<#if contextArguments?has_content>, <#else>) {</#if><#list contextArguments?keys as contextArgumentName>final ${contextArguments[contextArgumentName]?replace('$', '.')} ${contextArgumentName}<#if contextArgumentName_has_next>, <#else>) {</#if></#list>
<#list contextArguments?keys as contextArgumentName>
        this.${contextArgumentName} = ${contextArgumentName};
</#list>
    }

<#list contextArguments?keys as contextArgumentName>
    public ${contextArguments[contextArgumentName]?replace('$', '.')} get${contextArgumentName?cap_first}() {
        return ${contextArgumentName};
    }

</#list>
    /**
    * This should be used for non-webapp applications that are working
    * with test groups as those applications will not have a request and response,
    * such as boxcar services.
    */
    public ProctorResult getProctorResult(@Nonnull final ${groupsManagerClassName} groupsManager,
                                          @Nonnull final Identifiers identifiers) {
        return groupsManager.determineBuckets(identifiers<#if contextArguments?has_content>, <#else>);</#if>
<#list contextArguments?keys as contextArgumentName>
                                              ${contextArgumentName}<#if contextArgumentName_has_next>,<#else>);</#if>
</#list>
    }

    /**
    * This should be used for non-webapp applications that are working
    * with test groups as those applications will not have a request and response,
    * such as boxcar services.
    */
    public ProctorResult getProctorResult(@Nonnull final ${groupsManagerClassName} groupsManager,
                                          @Nonnull final Identifiers identifiers,
                                          @Nonnull final Map<String, Integer> forcedGroups) {
        return groupsManager.determineBuckets(identifiers, forcedGroups<#if contextArguments?has_content>, <#else>);</#if>
<#list contextArguments?keys as contextArgumentName>
                                              ${contextArgumentName}<#if contextArgumentName_has_next>,<#else>);</#if>
</#list>
    }

    public ProctorResult getProctorResult(@Nonnull final ${groupsManagerClassName} groupsManager,
                                          @Nonnull final HttpServletRequest request,
                                          @Nonnull final HttpServletResponse response,
                                          @Nonnull final Identifiers identifiers,
                                          final boolean allowForceGroups) {
        return groupsManager.determineBuckets(request, response, identifiers, allowForceGroups<#if contextArguments?has_content>, <#else>);</#if>
<#list contextArguments?keys as contextArgumentName>
                                              ${contextArgumentName}<#if contextArgumentName_has_next>,<#else>);</#if>
</#list>
    }
}