<%@ tag language="java" pageEncoding="UTF-8" description="Popup view of a definition" body-content="scriptless" %>
<%@ taglib prefix="ui" tagdir="/WEB-INF/tags/ui" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="proctor" uri="http://tags.indeed.com/proctor" %>
<%@ attribute name="definition" required="true" type="com.indeed.proctor.common.model.TestDefinition" %>
<c:forEach items="${definition.allocations}" var="allocation" varStatus="status">
<div class="media">
    <c:if test="${status.count > 1 || !empty allocation.rule}">
    <span class="img field-label pas"><c:if test="${empty allocation.rule}">[Default]</c:if><c:if test="${!empty allocation.rule}">${allocation.rule}</c:if></span>
    </c:if>
    <div class="bd pas">
        <ui:allocation-bar allocation="${allocation}" definition="${definition}" />
    </div>
</div>
</c:forEach>
<ui:expand-collapse more="show legend" less="hide" isMoreExpanded="false">
<ui:bucket-index definition="${definition}" />
</ui:expand-collapse>

