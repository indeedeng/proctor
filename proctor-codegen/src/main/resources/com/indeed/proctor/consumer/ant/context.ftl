<#-- @ftlvariable name="packageName" type="java.lang.String" -->
<#-- @ftlvariable name="mainClassName" type="java.lang.String" -->
<#-- @ftlvariable name="groupsManagerClassName" type="java.lang.String" -->
<#-- @ftlvariable name="contextArguments" type="java.util.Map<String, String>" -->

package ${packageName};

import com.google.common.base.Objects;
import com.google.common.base.Defaults;

import com.indeed.proctor.common.Identifiers;
import com.indeed.proctor.common.ProctorResult;

import javax.annotation.Generated;
import javax.annotation.Nonnull;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;

/*
 * GENERATED source; do not edit directly
 */
@Generated("com.indeed.proctor.consumer.gen.TestGroupsGenerator")
public class ${mainClassName} {
<#list contextArguments?keys as contextArgumentName>
    private final ${contextArguments[contextArgumentName]?replace('$', '.')} ${contextArgumentName};
</#list>

<#-- If there are context arguments, generate a fully-specified constructor and a private default constructor for the builder.
     Otherwise, rely on the default public constructor. -->
<#if contextArguments?has_content>
    public ${mainClassName}(<#list contextArguments?keys as contextArgumentName>final ${contextArguments[contextArgumentName]?replace('$', '.')} ${contextArgumentName}<#if contextArgumentName_has_next>, <#else>) {</#if></#list>
<#list contextArguments?keys as contextArgumentName>
        this.${contextArgumentName} = ${contextArgumentName};
</#list>
    }

    // For builder use only
    private ${mainClassName}() {
        <#-- TODO: It would be nice to allow the proctor specification to set default values for the arguments -->
        <#list contextArguments?keys as contextArgumentName>
        this.${contextArgumentName} = Defaults.defaultValue(${contextArguments[contextArgumentName]?replace('$', '.')}.class);
        </#list>
    }
</#if>

    private static final ${mainClassName} DEFAULT = new ${mainClassName}();
    public static ${mainClassName} getDefault() {
        return DEFAULT;
    }

<#list contextArguments?keys as contextArgumentName>
    public ${contextArguments[contextArgumentName]?replace('$', '.')} get${contextArgumentName?cap_first}() {
        return ${contextArgumentName};
    }

</#list>
    @Nonnull
    public String toString() {
        return "${mainClassName}{" +
<#list contextArguments?keys as contextArgumentName>
<#if contextArguments[contextArgumentName]?ends_with("$String") || contextArguments[contextArgumentName] == "String">
               "${contextArgumentName}='" + ${contextArgumentName} + <#if contextArgumentName_has_next>"', " +<#else>'\'' +</#if>
<#else>
               "${contextArgumentName}=" + ${contextArgumentName} +<#if contextArgumentName_has_next> ", " +</#if>
</#if>
</#list>
               '}';
    }

<#if contextArguments?has_content>
    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof ${mainClassName})) {
            return false;
        }

        return
<#list contextArguments?keys as contextArgumentName>
            Objects.equal(((${mainClassName}) obj).${contextArgumentName}, ${contextArgumentName})<#if contextArgumentName_has_next> &&<#else>;</#if>
</#list>
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(
<#list contextArguments?keys as contextArgumentName>
            this.${contextArgumentName}<#if contextArgumentName_has_next>, </#if>
</#list>
        );
    }

</#if>
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

<#if contextArguments?has_content>
    @Nonnull
    public static Builder newBuilder() { return new Builder(); }

    public static class Builder {
<#list contextArguments?keys as contextArgumentName>
        <#-- TODO: It would be nice to allow the proctor specification to set default values for the arguments -->
        private ${contextArguments[contextArgumentName]?replace('$', '.')} ${contextArgumentName} = Defaults.defaultValue(${contextArguments[contextArgumentName]?replace('$', '.')}.class);
</#list>
        private Builder() {}

<#list contextArguments?keys as contextArgumentName>
        @Nonnull
        public Builder set${contextArgumentName?cap_first}(final ${contextArguments[contextArgumentName]?replace('$', '.')} value) {
            this.${contextArgumentName} = value;
            return this;
        }

</#list>
        @Nonnull
        public ${mainClassName} build() {
            return new ${mainClassName}(
<#list contextArguments?keys as contextArgumentName>
                this.${contextArgumentName}<#if contextArgumentName_has_next>, </#if>
</#list>
            );
        }
    }
</#if>
}
