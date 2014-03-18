<%@ tag display-name="javascript" description="Standard template for proctor pages" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>

<%-- TODO: baseview model--%>
<%@ attribute name="useCompiledJavascript" required="false" type="java.lang.Boolean" %>
<%@ attribute name="compiledJavascriptSrc" required="false" type="java.lang.String" %>
<%@ attribute name="nonCompiledJavascriptSrc" required="false" type="java.lang.String" %>

<c:choose>
    <c:when test="${useCompiledJavascript}">
        <c:if test="${not empty compiledJavascriptSrc}">
            <script type="text/javascript" src="${compiledJavascriptSrc}"></script>
        </c:if>
    </c:when>
    <c:otherwise>
        <c:if test="${not empty nonCompiledJavascriptSrc}">
            <script type="text/javascript" src="/static/scripts/closure-library/closure/goog/base.js"></script>
            <script type="text/javascript" src="/static/scripts/app/deps.js"></script>
            <script type="text/javascript" src="${nonCompiledJavascriptSrc}"></script>
        </c:if>
    </c:otherwise>
</c:choose>
