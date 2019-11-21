package ${packageName};

import com.indeed.proctor.common.model.Payload;
import com.indeed.proctor.common.model.TestBucket;
import com.indeed.proctor.consumer.AbstractGroupsPayload;

import javax.annotation.Generated;
import javax.annotation.Nullable;

/*
* GENERATED source; do not edit directly
*/
@Generated("com.indeed.proctor.consumer.gen.TestGroupsGenerator")
public final class ${mainClassName} {
    <#-- Prevent instantiation -->
    private ${mainClassName}() { }

<#list testDefs as testDef>
  <#if (testDef.payloadJavaClass)??>
    <#if (testDef.isMap)??>
    public static final class ${testDef.name?cap_first} extends AbstractGroupsPayload {
        <#list testDef.nestedPayloadsList as nestedPayloadsMap>
        private final ${nestedPayloadsMap.value} ${nestedPayloadsMap.key};
        </#list>

        <#-- Constructor based on Bucket -->
        public ${testDef.name?cap_first}(@Nullable TestBucket bucket) {
            this(bucket != null ? bucket.getPayload() : null);
        }

        <#-- Constructor based on Payload -->
        public ${testDef.name?cap_first}(@Nullable Payload payload) {
            <#list testDef.nestedPayloadsList as nestedPayloadsMap>
            this.${nestedPayloadsMap.key} = super.convertTo${nestedPayloadsMap.payloadTypeName?cap_first}(payload, "${nestedPayloadsMap.key}");
            </#list>
        }

        <#list testDef.nestedPayloadsList as nestedPayloadsMap>
        public ${nestedPayloadsMap.value} get${nestedPayloadsMap.key?cap_first}(){
            return this.${nestedPayloadsMap.key};
        }
        </#list>
    }
    </#if>
  </#if>
</#list>
}
