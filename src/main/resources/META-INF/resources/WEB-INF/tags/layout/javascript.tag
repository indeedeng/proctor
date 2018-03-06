<%@ tag display-name="javascript" description="Standard template for proctor pages" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="proctor" uri="http://tags.indeed.com/proctor" %>

<%-- TODO: baseview model--%>
<%@ attribute name="useCompiledJavascript" required="false" type="java.lang.Boolean" %>
<%@ attribute name="compiledJavascriptSrc" required="false" type="java.lang.String" %>
<%@ attribute name="nonCompiledJavascriptSrc" required="false" type="java.lang.String" %>

<c:choose>
    <c:when test="${useCompiledJavascript}">
        <c:if test="${not empty compiledJavascriptSrc}">
            <script type="text/javascript" src=<proctor:filenameMapper filename="${compiledJavascriptSrc}"/>></script>
        </c:if>
    </c:when>
    <c:otherwise>
        <c:if test="${not empty nonCompiledJavascriptSrc}">
            <script type="text/javascript" src=<proctor:filenameMapper
                    filename="/static/scripts/closure-library/closure/goog/base.js"/>></script>
            <script type="text/javascript" src=<proctor:filenameMapper
                    filename="/static/scripts/app/deps.js"/>></script>
            <script type="text/javascript" src=<proctor:filenameMapper
                    filename="${nonCompiledJavascriptSrc}"/>></script>
        </c:if>
    </c:otherwise>
</c:choose>
