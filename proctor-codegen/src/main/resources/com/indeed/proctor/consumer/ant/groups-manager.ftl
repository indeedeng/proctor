package ${packageName};

import com.indeed.proctor.common.*;
import com.indeed.proctor.common.model.*;
import com.indeed.proctor.consumer.*;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Supplier;


/*
 * GENERATED source; do not edit directly
 */
public class ${mainClassName} extends AbstractGroupsManager {
    private static final Map<String, String> PROVIDED_CONTEXT;
    static {
        final Map<String, String> providedContext = new LinkedHashMap<String, String>();
        <#list contextArguments?keys as contextArgumentName>
        providedContext.put("${contextArgumentName}", "${contextArguments[contextArgumentName]}");
        </#list>
        PROVIDED_CONTEXT = Collections.unmodifiableMap(providedContext);
    }

    public ${mainClassName}(final Supplier<Proctor> proctorSource) {
        super(proctorSource);
    }

    /**
     * This should be used for non-webapp applications that are working
     * with test groups as those applications will not have a request and response,
     * such as boxcar services.
     * @deprecated Use the one that takes a Map<TestType, String> instead
     */
    public ProctorResult determineBuckets(final TestType testType, final String identifier<#if contextArguments?has_content>,<#else>) {</#if>
<#list contextArguments?keys as contextArgumentName>
                                    final ${contextArguments[contextArgumentName]?replace('$', '.')} ${contextArgumentName}<#if contextArgumentName_has_next>,<#else>) {</#if>
</#list>
        <#if contextArguments?has_content>
        final Map<String, Object> context = new HashMap<String, Object>();
            <#list contextArguments?keys as contextArgumentName>
        context.put("${contextArgumentName}", ${contextArgumentName});
            </#list>
        <#else>
        final Map<String, Object> context = Collections.emptyMap();
        </#if>
        return super.determineBucketsInternal(testType, identifier, context);
    }

    /**
     * This should be used for non-webapp applications that are working
     * with test groups as those applications will not have a request and response,
     * such as boxcar services.
     */
    public ProctorResult determineBuckets(final Identifiers identifiers<#if contextArguments?has_content>,<#else>) {</#if>
<#list contextArguments?keys as contextArgumentName>
                                    final ${contextArguments[contextArgumentName]?replace('$', '.')} ${contextArgumentName}<#if contextArgumentName_has_next>,<#else>) {</#if>
</#list>
        <#if contextArguments?has_content>
        final Map<String, Object> context = new HashMap<String, Object>();
            <#list contextArguments?keys as contextArgumentName>
        context.put("${contextArgumentName}", ${contextArgumentName});
            </#list>
        <#else>
        final Map<String, Object> context = Collections.emptyMap();
        </#if>
        return super.determineBucketsInternal(identifiers, context);
    }

    /**
     * This should be used for non-webapp applications that are working
     * with test groups as those applications will not have a request and response,
     * such as boxcar services.
     */
    public ProctorResult determineBuckets(final Identifiers identifiers,
                                            final Map<String, Integer> forcedGroups<#if contextArguments?has_content>,<#else>) {</#if>
<#list contextArguments?keys as contextArgumentName>
                                    final ${contextArguments[contextArgumentName]?replace('$', '.')} ${contextArgumentName}<#if contextArgumentName_has_next>,<#else>) {</#if>
</#list>
        <#if contextArguments?has_content>
        final Map<String, Object> context = new HashMap<String, Object>();
            <#list contextArguments?keys as contextArgumentName>
        context.put("${contextArgumentName}", ${contextArgumentName});
            </#list>
        <#else>
        final Map<String, Object> context = Collections.emptyMap();
        </#if>
        return super.determineBucketsInternal(identifiers, context, forcedGroups);
    }

    /*
     * @deprecated Use the one that takes a Map<TestType, String> instead
     */
    public ProctorResult determineBuckets(final HttpServletRequest request, final HttpServletResponse response,
                                            final TestType testType, final String identifier, final boolean allowForcedGroups<#if contextArguments?has_content>,<#else>) {</#if>
<#list contextArguments?keys as contextArgumentName>
                                            final ${contextArguments[contextArgumentName]?replace('$', '.')} ${contextArgumentName}<#if contextArgumentName_has_next>,<#else>) {</#if>
</#list>
        final Identifiers identifiers = new Identifiers(testType, identifier);
        return determineBuckets(request, response, identifiers, allowForcedGroups
<#list contextArguments?keys as contextArgumentName>
                , ${contextArgumentName}
</#list>
                );
    }

    public ProctorResult determineBuckets(final HttpServletRequest request, final HttpServletResponse response,
                                            final Identifiers identifiers, final boolean allowForcedGroups<#if contextArguments?has_content>,<#else>) {</#if>
<#list contextArguments?keys as contextArgumentName>
                                            final ${contextArguments[contextArgumentName]?replace('$', '.')} ${contextArgumentName}<#if contextArgumentName_has_next>,<#else>) {</#if>
</#list>
        <#if contextArguments?has_content>
        final Map<String, Object> context = new HashMap<String, Object>();
            <#list contextArguments?keys as contextArgumentName>
        context.put("${contextArgumentName}", ${contextArgumentName});
            </#list>
        <#else>
        final Map<String, Object> context = Collections.emptyMap();
        </#if>
        return super.determineBucketsInternal(request, response, identifiers, context, allowForcedGroups);
    }

    private static final Map<String, TestBucket> DEFAULT_BUCKET_VALUES = constructDefaultBucketValuesMap();
    private static Map<String, TestBucket> constructDefaultBucketValuesMap() {
        final Map<String, TestBucket> defaultBucketValues = new HashMap<String, TestBucket>();
        <#list testDefs as testDef>
            defaultBucketValues.put("${testDef.name}", new TestBucket("fallback", ${testDef.defaultValue}, "fallback value; something is broken", null));
        </#list>
        return Collections.unmodifiableMap(defaultBucketValues);
    }
    @Override
    protected Map<String, TestBucket> getDefaultBucketValues() {
        return DEFAULT_BUCKET_VALUES;
    }

    @Override
    public Map<String, String> getProvidedContext() {
        return PROVIDED_CONTEXT;
    }
}
