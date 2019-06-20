<%@ tag language="java" pageEncoding="UTF-8" description="Popup view of a definition" body-content="scriptless" trimDirectiveWhitespaces="true" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="proctor" uri="http://tags.indeed.com/proctor" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ attribute name="definition" required="true" type="com.indeed.proctor.common.model.TestDefinition" %>
<%@ attribute name="allocation" required="true" type="com.indeed.proctor.common.model.Allocation" %>
<%@ attribute name="extraCssClass" required="false" type="java.lang.String" rtexprvalue="true" %>
<div class="ui-allocation-bar ${extraCssClass}">
<c:forEach items="${allocation.ranges}" var="range">
    <c:if test="${range.length > 0}">
    <fmt:formatNumber var="percentWidth" value="${range.length}" type="PERCENT" maxFractionDigits="3" />
    <fmt:formatNumber var="percent" value="${range.length}" type="PERCENT" maxFractionDigits="2"/>
    <c:set var="tstBucket" value="${proctor:getTestBucketForRange(definition, range)}"/>
    <span title="${fn:escapeXml(tstBucket.name)} - ${percent}" class="ui-allocation-range ui-color${(1+range.bucketValue) % 12}" style="width: ${percentWidth};" ><span class="ui-allocation-range-lbl">${fn:escapeXml(tstBucket.name)} - ${percent}</span></span>
    </c:if>
</c:forEach>
</div>
