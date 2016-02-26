<%@ page import="com.indeed.proctor.webapp.extensions.renderer.MatrixListPageRenderer" %>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="ui" tagdir="/WEB-INF/tags/ui" %>
<%@ taglib prefix="layout" tagdir="/WEB-INF/tags/layout" %>
<%@ taglib prefix="proctor" uri="http://tags.indeed.com/proctor" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%--@elvariable id="session" type="com.indeed.proctor.webapp.model.SessionViewModel"--%>
<%--@elvariable id="testMatrixVersion" type="com.indeed.proctor.webapp.TestMatrixVersion"--%>
<%--@elvariable id="testMatrixDefinition" type="java.lang.String"--%>
<%--@elvariable id="branch" type="com.indeed.proctor.webapp.db.Environment"--%>
<c:set var="testMatrix" value="${testMatrixVersion.testMatrixDefinition}"/>
<layout:base title="Proctor - current test matrix" session="${session}">
        <h2>${branch.name} test matrix</h2>
        <c:forEach items="${testMatrix.tests}" var="test">
            <c:set var="testDefinition" value="${test.value}" />
            <div class="panel radius">
            <ui:grid-row extraCssClass="ui-test-definition">
                <ui:grid-columns width="three">
                    <h6 class="mtn"><a class="breakAll" href="/proctor/definition/${proctor:urlencode(test.key)}?branch=${proctor:urlencode(branch.name)}">${fn:escapeXml(test.key)}</a></h6>
                    <ul class="button-group radius">
                        <li><a class="tiny button secondary radius" href="/proctor/definition/${proctor:urlencode(test.key)}/edit?branch=${proctor:urlencode(branch.name)}">edit</a></li>
                        <%--<li><a class="tiny button secondary radius" href="/proctor/definition/${test.key}/history">history</a></li>--%>
                        <li><a class="tiny button secondary radius" href="/proctor/definition/${proctor:urlencode(test.key)}?branch=${proctor:urlencode(branch.name)}">details</a></li>
                    </ul>
                    <proctor:renderMatrixListPageInjectionTemplates position="<%=MatrixListPageRenderer.MatrixListPagePosition.LINK%>" testName="${proctor:urlencode(test.key)}" testMatrixVersion="${testMatrixVersion}" testDefinition="${testDefinition}"/>
                </ui:grid-columns>
                <ui:grid-columns width="nine">
                    <div class="def-description">
                        <proctor:formatCommitMessageDisplay commitMessage="${testDefinition.description}"/>
                    </div>
                    <c:if test="${!empty testDefinition.rule}"><div class="rule">rule: ${fn:escapeXml(testDefinition.rule)}</div></c:if>
                    <ui:allocations definition="${testDefinition}"/>
                </ui:grid-columns>
            </ui:grid-row>
            </div>
        </c:forEach>

    <layout:javascript
            useCompiledJavascript="${session.useCompiledJavaScript}"
            compiledJavascriptSrc="/static/scripts/app/matrix-list-compiled.js"
            nonCompiledJavascriptSrc="/static/scripts/app/matrix-list.js"/>
    <script type="text/javascript">
        //<![CDATA[
        indeed.proctor.app.matrix.list.start(${testMatrixDefinition});
        //]]>
    </script>
<!--
<c:out value="${testMatrixDefinition}" />
-->
</layout:base>
