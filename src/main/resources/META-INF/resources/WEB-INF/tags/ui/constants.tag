<%@ tag language="java" pageEncoding="UTF-8" description="Dump out a map of constants" body-content="scriptless" %>
<%@ attribute name="constants" required="true" type="java.util.Map" %>

<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<c:if test="${empty constants}">
[None]
</c:if>
<c:if test="${! empty constants}">
<table>
    <thead>
        <tr>
            <th>Variable</th>
            <th>Value</th>
        </tr>
    </thead>
    <tbody>
        <c:forEach items="${constants}" var="constantVariable">
        <tr>
            <td>${fn:escapeXml(constantVariable.key)}</td>
            <td>${fn:escapeXml(constantVariable.value)}</td>
        </tr>
        </c:forEach>
    </tbody>
</table>
</c:if>
