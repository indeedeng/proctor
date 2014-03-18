<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="ui" tagdir="/WEB-INF/tags/ui" %>
<%@ taglib prefix="layout" tagdir="/WEB-INF/tags/layout" %>
<%@ taglib prefix="proctor" uri="http://tags.indeed.com/proctor" %>
<%--@elvariable id="session" type="com.indeed.proctor.webapp.model.SessionViewModel"--%>
<%--@elvariable id="tests" type="java.util.Map<String, com.indeed.proctor.webapp.controllers.ProctorController.CompatibilityRow>"--%>
<%--@elvariable id="devMatrix" type="com.indeed.proctor.webapp.TestMatrixVersion"--%>
<%--@elvariable id="qaMatrix" type="com.indeed.proctor.webapp.TestMatrixVersion"--%>
<%--@elvariable id="productionMatrix" type="com.indeed.proctor.webapp.TestMatrixVersion"--%>
<layout:base title="Proctor - current test matrix" session="${session}">
    <table class="w100">
        <thead>
            <tr>
                <th><h6>Name</h6></th>
                <th>DEV (webapp)</th>
                <th>QA (webapp)</th>
                <th>PRODUCTION (webapp)</th>
            </tr>
        </thead>
        <c:forEach items="${tests}" var="entry">
            <c:set var="testName" value="${entry.key}"/>
            <c:set var="row" value="${entry.value}"/>
            <c:set var="inDevMatrix" value="${proctor:containsKey(devMatrix.testMatrixDefinition.tests, testName)}"/>
            <c:set var="inQaMatrix" value="${proctor:containsKey(qaMatrix.testMatrixDefinition.tests, testName)}"/>
            <c:set var="inProductionMatrix" value="${proctor:containsKey(productionMatrix.testMatrixDefinition.tests, testName)}"/>
            <tr>
                <td>
                    <a href="/proctor/definition/${testName}"><h6>${testName}</h6></a>
                </td>
                <td>
                    <ul class="nice">
                        <c:if test="${inDevMatrix}"><li><a class="label" href="/proctor/definition/${testName}">TRUNK MATRIX</a></li></c:if>
                        <c:if test="${empty row.dev}"><li>[no webapps]</li></c:if>
                        <c:forEach items="${row.dev}" var="version">
                            <li><ui:compatible-result version="${version}" branch="trunk" /></li>
                        </c:forEach>
                    </ul>
                </td>
                <td>
                    <ul class="nice">
                        <c:if test="${inQaMatrix}"><li><a class="label" href="/proctor/definition/${testName}?branch=qa">QA MATRIX</a></li></c:if>
                        <c:if test="${empty row.qa}"><li>[no webapps]</li></c:if>
                        <c:forEach items="${row.qa}" var="version">
                            <li><ui:compatible-result version="${version}" branch="qa" /></li>
                        </c:forEach>
                    </ul>
                </td>
                <td>
                    <ul class="nice">
                        <c:if test="${inProductionMatrix}"><li><a class="label" href="/proctor/definition/${testName}?branch=production">PRODUCTION MATRIX</a></li></c:if>
                        <c:if test="${empty row.production}"><li>[no webapps]</li></c:if>
                        <c:forEach items="${row.production}" var="version">
                            <li><ui:compatible-result version="${version}" branch="production" /></li>
                        </c:forEach>
                    </ul>
                </td>
            </tr>
        </c:forEach>
    </table>

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
