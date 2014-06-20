<%@ tag language="java" pageEncoding="UTF-8" description="Popup view of a definition" body-content="scriptless" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="proctor" uri="http://tags.indeed.com/proctor" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ attribute name="definition" required="true" type="com.indeed.proctor.common.model.TestDefinition" %>
<%@ attribute name="allocation" required="true" type="com.indeed.proctor.common.model.Allocation" %>
<table class="w100 fixed">
    <tr>
        <th>Name</th>
        <th class="one">Ratio</th>
    </tr>
    <c:forEach items="${allocation.ranges}" var="range">
        <c:set var="tstBucket" value="${proctor:getTestBucketForRange(definition, range)}"/>
        <tr>
            <td>${fn:escapeXml(tstBucket.name)}</td>
            <td class="one">${range.length}</td>
        </tr>
    </c:forEach>
</table>
