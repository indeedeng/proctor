<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="ui" tagdir="/WEB-INF/tags/ui" %>
<%@ taglib prefix="layout" tagdir="/WEB-INF/tags/layout" %>
<%@ taglib prefix="proctor" uri="http://tags.indeed.com/proctor" %>
<%--@elvariable id="session" type="com.indeed.proctor.webapp.model.SessionViewModel"--%>
<%--@elvariable id="compatibilityMap" type="java.util.Map<com.indeed.proctor.webapp.db.Environment, com.indeed.proctor.webapp.controllers.ProctorController.CompatibilityRow>"--%>
<layout:base title="Proctor - compatibility" session="${session}" >


<c:forEach items="${compatibilityMap}" var="result">
    <c:set var="environment" value="${result.key}" />
    <c:set var="row" value="${result.value}" />

    <table class="w100 fixed">
        <thead>
            <tr>
                <th style="width:25%;">Matrix</th>
                <c:if test="${!empty row.dev}">
                    <th style="width:25%;">DEV (webapp)</th>
                </c:if>
                <th style="width:25%;">QA (webapp)</th>
                <th style="width:25%;">PRODUCTION (webapp)</th>
            </tr>
        </thead>
        <tbody>
            <tr>
                <td><h6>${environment.name}</h6></td>
                <c:if test="${!empty row.dev}">
                    <td>
                        <ul class="nice">
                            <c:forEach items="${row.dev}" var="version">
                                <li><ui:compatible-result version="${version}" branch="trunk" /></li>
                            </c:forEach>
                        </ul>
                    </td>
                </c:if>
                <td>
                    <c:if test="${empty row.qa}">[NONE]</c:if>
                    <c:if test="${!empty row.qa}">
                        <ul class="nice">
                            <c:forEach items="${row.qa}" var="version">
                                <li><ui:compatible-result version="${version}" branch="qa" /></li>
                            </c:forEach>
                        </ul>
                    </c:if>
                </td>
                <td>
                    <c:if test="${empty row.production}">[NONE]</c:if>
                    <c:if test="${!empty row.production}">
                        <ul class="nice">
                            <c:forEach items="${row.production}" var="version">
                                <li><ui:compatible-result version="${version}" branch="production" /></li>
                            </c:forEach>
                        </ul>
                    </c:if>
                </td>
            </tr>
        </tbody>
    </table>
</c:forEach>
    <layout:javascript
            useCompiledJavascript="${session.useCompiledJavaScript}"
            compiledJavascriptSrc="/static/scripts/app/matrix-list-compiled.js"
            nonCompiledJavascriptSrc="/static/scripts/app/matrix-list.js"/>
    <script type="text/javascript">
        //<![CDATA[
        indeed.proctor.app.matrix.usage.start();
        //]]>
    </script>

</layout:base>
