package ${packageName};

import com.indeed.proctor.common.model.TestBucket;
import com.indeed.proctor.consumer.AbstractGroupsPayload;

import javax.annotation.Nullable;

/*
* GENERATED source; do not edit directly
*/
public final class ${mainClassName} {
    private ${mainClassName}() { }

    <#list testDefs as testDef>
    <#if (testDef.payloadJavaClass)??>
    <#if (testDef.isMap)??>
    public static final class ${testDef.name?cap_first} extends AbstractGroupsPayload {
        <#list testDef.nestedPayloadsList as nestedPayloadsMap>
        private final ${nestedPayloadsMap.value} ${nestedPayloadsMap.key};
        </#list>
        public ${testDef.name?cap_first} (@Nullable TestBucket bucket) {
            <#list testDef.nestedPayloadsList as nestedPayloadsMap>
            this.${nestedPayloadsMap.key} = super.convertTo${nestedPayloadsMap.payloadTypeName?cap_first}(bucket, "${nestedPayloadsMap.key}");
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
