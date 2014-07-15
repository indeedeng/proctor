package ${packageName};

import com.indeed.proctor.common.*;
import com.indeed.proctor.common.model.Payload;
import com.indeed.proctor.common.model.TestBucket;
import com.indeed.proctor.consumer.*;
import javax.annotation.Nullable;
import javax.annotation.Nonnull;

/*
 * GENERATED source; do not edit directly
 * (but you can extend me.  you'll want to override {@link #toString()}, using {@link #buildTestGroupString()} or {@link #appendTestGroups(StringBuilder)} instead)
 */
public class ${mainClassName} extends AbstractGroups {

    public static final ${mainClassName} EMPTY = new ${mainClassName}(ProctorResult.EMPTY);

    public ${mainClassName}(final ProctorResult proctorResult) {
        super(proctorResult);
    }

    public static Bucket<${testEnumName}>[] getBuckets(final ${testEnumName} test) {
        switch (test) {
            <#list testDefs as testDef>
            case ${testDef.enumName}:
                return ${testDef.javaClassName}.values();
            </#list>
        }
        return null;
    }

    public enum ${testEnumName} {
        <#list testDefs as testDef>
        ${testDef.enumName}("${testDef.normalizedName}")<#if testDef_has_next>,<#else>;</#if>
        </#list>
        ; // fix compilation if no tests

        private final String name;
        private ${testEnumName}(final String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }
    }

<#list testDefs as testDef>
    public enum ${testDef.javaClassName} implements Bucket<${testEnumName}> {
        <#list testDef.buckets as bucket>
        ${bucket.enumName}(${bucket.value}, "${bucket.normalizedName}")<#if bucket_has_next>,<#else>;</#if>
        </#list>

        private final int value;
        private final String name;
        private final String fullName;
        private ${testDef.javaClassName}(final int value, final String name) {
            this.value = value;
            this.name = name;
            this.fullName = getTest().getName() + "-" + name;
        }

        @Override
        public ${testEnumName} getTest() {
            return ${testEnumName}.${testDef.enumName};
        }

        @Override
        public int getValue() {
            return value;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public String getFullName() {
            return fullName;
        }

    <#list testDef.buckets as bucket>
        <#if testDef.defaultValue == bucket.value>
        public static ${testDef.javaClassName} getFallback() {
            return ${testDef.javaClassName}.${bucket.enumName};
        }
        </#if>
    </#list>
    }

</#list>
<#list testDefs as testDef>
    @Nonnull
    public ${testDef.javaClassName} get${testDef.javaClassName}() {
        for (final ${testDef.javaClassName} bucket : ${testDef.javaClassName}.values()) {
            final String testName = Test.${testDef.enumName}.getName();
            if (isBucketActive(testName, bucket.getValue(), ${testDef.defaultValue})) {
                return bucket;
            }
        }

        // Safe to throw NPE here because the code generator ensures that the default value
        //  is a valid bucket in the test.
        throw new NullPointerException("No fallback bucket found for '${testDef.name}'");
    }

    /**
      * @deprecated Use {@link #get${testDef.javaClassName}Value()} instead
      */
    public int get${testDef.javaClassName}Value(final int defaultValue) {
        return getValue(${testEnumName}.${testDef.enumName}.getName(), defaultValue);
    }

    public int get${testDef.javaClassName}Value() {
        return getValue(${testEnumName}.${testDef.enumName}.getName(), ${testDef.defaultValue});
    }

    <#if (testDef.payloadJavaClass)??>
    public @Nullable ${testDef.payloadJavaClass} get${testDef.javaClassName}Payload() {
        final Payload payload = getPayload(${testEnumName}.${testDef.enumName}.getName(), ${testDef.javaClassName}.getFallback());
        return payload.${testDef.payloadAccessorName}();
    }

    public @Nullable ${testDef.payloadJavaClass} get${testDef.javaClassName}PayloadForBucket(final ${testDef.javaClassName} targetBucket) {
        final @Nullable TestBucket bucket = getTestBucketForBucket(${testEnumName}.${testDef.enumName}.getName(), targetBucket);
        if (bucket == null) {
            return null;
        }
        final Payload payload = bucket.getPayload();
        if (payload == null) {
            return null;
        }
        return payload.${testDef.payloadAccessorName}();
    }
    </#if>

    <#if (testDef.description)??>
    public @Nullable String get${testDef.javaClassName}Description() {
        return "${testDef.description}";
    }
    </#if>

<#list testDef.buckets as bucket>
    public boolean is${testDef.javaClassName}${bucket.javaClassName}() {
        final String testName = Test.${testDef.enumName}.getName();
        final int bucketValue = ${testDef.javaClassName}.${bucket.enumName}.getValue();
        return isBucketActive(testName, bucketValue, ${testDef.defaultValue});
    }
<#if bucket_has_next || testDef_has_next>

</#if>
</#list>
</#list>
}
