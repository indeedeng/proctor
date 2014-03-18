<%@ tag language="java" pageEncoding="UTF-8" description="Popup view of a definition" body-content="scriptless" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="proctor" uri="http://tags.indeed.com/proctor" %>
<%@ attribute name="definition" required="true" type="com.indeed.proctor.common.model.TestDefinition" %>
<%@ taglib prefix="ui" tagdir="/WEB-INF/tags/ui" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
<style type="text/css">
    .payloads-hidden
    .js-bucket-payload {display:none}
</style>
<c:set var="hasPayload" value="${!empty definition.buckets && !empty definition.buckets[0].payload}"/>
<%-- if .payload is null, payloadType will be set to "none" --%>
<c:set var="payloadType" value="${proctor:printPayloadType(definition.buckets[0].payload)}"/>
<div class="js-bucket-editor <c:if test="${!hasPayload}">payloads-hidden</c:if>">
    <div class="panel">
        <ui:grid-row >
            <ui:grid-columns width="one"> </ui:grid-columns>
            <ui:grid-columns width="two"><label class="right inline">Payload type:</label></ui:grid-columns>
            <%-- Set up the selector of all payload types --%>
            <ui:grid-columns width="seven"><span class="inline"><select class="js-payload-type" name="payload-type"
                <c:if test="${hasPayload}">disabled="disabled"</c:if> >
                <option value="none" <c:if test="${'none' == payloadType}">selected="selected"</c:if> >none</option>
                <c:forEach items="${proctor:allPayloadTypeStrings()}" var="optionType" varStatus="status">
                    <option value="${optionType}" <c:if test="${optionType == payloadType}">selected="selected"</c:if> >${optionType}</option>
                </c:forEach>
            </select></span></ui:grid-columns>
            <ui:grid-columns width="two"> </ui:grid-columns>
        </ui:grid-row>
        <ui:grid-row >
            <ui:grid-columns width="one"><h6>Value</h6></ui:grid-columns>
            <ui:grid-columns width="two"><h6>Name</h6></ui:grid-columns>
            <ui:grid-columns width="nine"><h6>Description</h6></ui:grid-columns>
        </ui:grid-row>
        <div class="js-buckets">
            <c:forEach items="${definition.buckets}" var="bucket" varStatus="status">
                <ui:grid-row extraCssClass="ui-bucket-row js-bucket-row">
                    <ui:grid-columns width="ten">
                        <ui:grid-row>
                            <ui:grid-columns width="one"><input class="js-bucket-value json" placeholder="Value" type="text" value="${bucket.value}" name="buckets[${status.index}].value"/></ui:grid-columns>
                            <ui:grid-columns width="two"><input class="js-bucket-name json" placeholder="Name" type="text" value="${bucket.name}" name="buckets[${status.index}].name"/></ui:grid-columns>
                            <ui:grid-columns width="nine"><input class="js-bucket-description json" placeholder="Description" type="text" value="${bucket.description}" name="buckets[${status.index}].description"/></ui:grid-columns>
                        </ui:grid-row>
                        <ui:grid-row>
                            <ui:grid-columns width="one"> </ui:grid-columns>
                            <ui:grid-columns width="eleven"><textarea rows="1" cols="11" class="js-bucket-payload json" name="buckets[${status.index}].payload.${payloadType}" <c:if test="${!hasPayload}">disabled</c:if> data-json-type="raw"><c:if test="${hasPayload}">${proctor:prettyPrintJSONPayloadContents(bucket.payload)}</c:if></textarea></ui:grid-columns>
                        </ui:grid-row>
                    </ui:grid-columns>
                    <ui:grid-columns width="two"><a class="js-delete-bucket tiny button secondary radius" href="#">Delete</a></ui:grid-columns>
                </ui:grid-row>
            </c:forEach>
         </div>
        <div class="panel ui-panel-buttons">
        <ui:grid-row extraCssClass="ui-bucket-add-row js-add-bucket-row">
            <ui:grid-columns width="ten">
            <ui:grid-row>
                <ui:grid-columns width="one"><input class="js-bucket-value newbucket" placeholder="Value" type="text" value="" name="add-bucket.value"/></ui:grid-columns>
                <ui:grid-columns width="two"><input class="js-bucket-name newbucket" placeholder="Name" type="text" value="" name="add-bucket.name"/></ui:grid-columns>
                <ui:grid-columns width="nine"><input class="js-bucket-description newbucket" placeholder="Description" type="text" value="" name="add-bucket.description"/></ui:grid-columns>
            </ui:grid-row>
            <ui:grid-row>
                <ui:grid-columns width="one"> </ui:grid-columns>
                <ui:grid-columns width="nine">
                    <textarea rows="1" cols="9" class="js-bucket-payload newbucket" placeholder="Payload" name="add-bucket.payload" data-json-type="raw"></textarea>
                </ui:grid-columns>
            </ui:grid-row>
            </ui:grid-columns>
            <ui:grid-columns width="two"><a class="js-add-bucket small button secondary radius" href="#">Add Bucket</a></ui:grid-columns>
        </ui:grid-row>
        </div>

    </div>
</div>
