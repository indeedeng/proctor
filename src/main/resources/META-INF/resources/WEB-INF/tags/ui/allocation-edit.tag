<%@ tag language="java" pageEncoding="UTF-8" description="Popup view of a definition" body-content="scriptless" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="proctor" uri="http://tags.indeed.com/proctor" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="ui" tagdir="/WEB-INF/tags/ui" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ attribute name="definition" required="true" type="com.indeed.proctor.common.model.TestDefinition" %>
<%@ attribute name="allocation" required="true" type="com.indeed.proctor.common.model.Allocation" %>
<%@ attribute name="allocationIndex" required="true" type="java.lang.Integer" %>
<div class="js-allocation-editor">
    <div class="panel">
        <c:set var="isDefault" value="${empty allocation.rule && allocationIndex == fn:length(definition.allocations) - 1}"/>
        <div class="js-rule-container <c:if test="${isDefault}">hide</c:if>">
            <ui:grid-row>
                <ui:grid-columns width="two">
                    <label class="inline">Rule</label>
                </ui:grid-columns>
                <ui:grid-columns width="ten">
                    <input type="text" class="json js-input-rule" name="allocations[${allocationIndex}].rule" value="${fn:escapeXml(allocation.rule)}" />
                </ui:grid-columns>
            </ui:grid-row>
        </div>
        <div class="js-allocations">
            <c:forEach items="${allocation.ranges}" var="range" varStatus="status">
            <ui:grid-row extraCssClass="js-ratio-row">
                <ui:grid-columns width="six">
                    <span class="inline"><ui:bucket-select extraCssClass="json" definition="${definition}" selectedBucketValue="${range.bucketValue}" inputName="allocations[${allocationIndex}].ranges[${status.index}].bucketValue"/></span>
                </ui:grid-columns>
                <fmt:formatNumber value="${range.length}" type="number" pattern="0.0" maxFractionDigits="10" var="formattedLength" />
                <ui:grid-columns width="two"><input class="json" type="text" name="allocations[${allocationIndex}].ranges[${status.index}].length" value="${formattedLength}" /></ui:grid-columns>
                <ui:grid-columns width="two"><span class="inline ui-allocation-percent"><fmt:formatNumber value="${range.length}" type="PERCENT" maxFractionDigits="2"/></span></ui:grid-columns>
                <ui:grid-columns width="one"><a class="js-delete-range tiny button secondary radius" href="#">delete</a></ui:grid-columns>
                <ui:grid-columns width="one"><a class="js-split-range tiny button secondary radius" href="#">split</a></ui:grid-columns>
            </ui:grid-row>
            </c:forEach>
        </div>
        <ui:grid-row>
            <ui:grid-columns width="two" >
                <label class="inline">New:</label>
            </ui:grid-columns>
            <ui:grid-columns width="ten" >
                <ui:allocation-bar allocation="${allocation}" definition="${definition}" extraCssClass="js-new-allocation-bar"/>
            </ui:grid-columns>
        </ui:grid-row>
        <ui:grid-row>
            <ui:grid-columns width="two" ></ui:grid-columns>
            <ui:grid-columns width="ten" >
                <div style="display:none;" class="allocations-msg-container"></div>
            </ui:grid-columns>
        </ui:grid-row>
        <ui:grid-row>
            <ui:grid-columns width="two" >
                <label class="inline">Previous:</label>
            </ui:grid-columns>
            <ui:grid-columns width="ten" >
                <ui:allocation-bar allocation="${allocation}" definition="${definition}" />
            </ui:grid-columns>
        </ui:grid-row>
        <div class="panel ui-panel-buttons">
            <ui:grid-row extraCssClass="js-add-ratio-row">
                <ui:grid-columns width="six"><span class="inline"><ui:bucket-select definition="${definition}" selectedBucketValue="-1" inputName="add-bucket.bucketValue"/></span></ui:grid-columns>
                <ui:grid-columns width="two"><input placeholder="bucket value (0.0 to 1.0)" type="text" value="0.0" name="add-bucket.length"/></ui:grid-columns>
                <ui:grid-columns width="two"><a class="js-add-ratio small button secondary radius" href="#">Add Ratio</a></ui:grid-columns>
            </ui:grid-row>
        </div>
        <ui:grid-row>
            <ui:grid-columns width="three" ><a class="js-delete-allocation small button secondary radius <c:if test="${isDefault}">hide</c:if>" href="#">Delete Allocation</a></ui:grid-columns>
            <ui:grid-columns width="two" ><a class="js-add-allocation small button secondary radius" href="#">Add Allocation</a></ui:grid-columns>
        </ui:grid-row>
    </div>
</div>