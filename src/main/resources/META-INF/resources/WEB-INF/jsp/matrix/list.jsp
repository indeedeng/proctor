<%@ page import="com.indeed.proctor.webapp.extensions.renderer.MatrixListPageRenderer" %>
<%@ page contentType="text/html;charset=UTF-8" language="java" trimDirectiveWhitespaces="true" %>
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
        <div id="filter-container">
            <ui:grid-row>
                <ui:grid-columns width="two">
                    <label class="inline">
                        <select class="js-filter-type">
                            <option value="all" selected="selected">Any field</option>
                            <option value="testName">Test name</option>
                            <option value="description">Description</option>
                            <option value="rule">Rule</option>
                            <option value="bucket">Bucket name</option>
                            <option value="bucketDescription">Bucket description</option>
                        </select>
                    </label>
                </ui:grid-columns>
                <ui:grid-columns width="ten">
                    <label>
                        <input type="text" name="test name" class="js-filter-text" placeholder="Filter">
                    </label>
                </ui:grid-columns>
            </ui:grid-row>
            <div class="js-filter-active">
                <label>
                    <input type="radio" name="filterActive" value="all" checked>
                    All
                </label>
                <label>
                    <input type="radio" name="filterActive" value="active">
                    <span>Active: Some allocations are occupied by multiple buckets</span>
                </label>
                <label>
                    <input type="radio" name="filterActive" value="inactive">
                    <span>Inactive: All allocations are occupied by 100% buckets</span>
                </label>
            </div>
            <p>
                Showing <span class="js-filter-num-matched">-</span> / <span class="js-filter-num-all">-</span> tests
                ordered by <select class="js-filter-sorted-by"></select>
            </p>
        </div>
        <div id="test-container">
        <c:forEach items="${testMatrix.tests}" var="test">
            <c:set var="testDefinition" value="${test.value}" />
            <div class="panel radius" data-updated="${updatedTimeMap.get(test.key)}">
            <!-- todo move the data-updated attribute to the ui-test-definition to make it a bit more semantic -->
            <ui:grid-row extraCssClass="ui-test-definition">
                <ui:grid-columns width="three">
                    <h6 class="mtn"><a class="ui-test-name" href="/proctor/definition/${proctor:urlencode(test.key)}?branch=${proctor:urlencode(branch.name)}">${fn:escapeXml(test.key)}</a></h6>
                    <ul class="button-group radius">
                        <li><a class="tiny button secondary radius" href="/proctor/definition/${proctor:urlencode(test.key)}/edit?branch=${proctor:urlencode(branch.name)}">edit</a></li>
                        <%--<li><a class="tiny button secondary radius" href="/proctor/definition/${test.key}/history">history</a></li>--%>
                        <li><a class="tiny button secondary radius" href="/proctor/definition/${proctor:urlencode(test.key)}?branch=${proctor:urlencode(branch.name)}">details</a></li>
                    </ul>
                    <proctor:renderMatrixListPageInjectionTemplates position="<%=MatrixListPageRenderer.MatrixListPagePosition.LINK%>" testName="${proctor:urlencode(test.key)}" testMatrixVersion="${testMatrixVersion}" testDefinition="${testDefinition}"/>
                </ui:grid-columns>
                <ui:grid-columns width="eight">
                    <div class="def-description">
                        <proctor:formatCommitMessageDisplay commitMessage="${testDefinition.description}"/>
                    </div>
                    <c:if test="${!empty testDefinition.rule}"><div class="rule">rule: ${fn:escapeXml(testDefinition.rule)}</div></c:if>
                    <ui:allocations definition="${testDefinition}"/>
                </ui:grid-columns>
                <ui:grid-columns width="one">
                    <div class="favorite" data-testname="${fn:escapeXml(test.key)}"></div>
                </ui:grid-columns>
            </ui:grid-row>
            </div>
        </c:forEach>
        </div>

    <layout:javascript
            useCompiledJavascript="${session.useCompiledJavaScript}"
            compiledJavascriptSrc="/static/scripts/app/matrix-list-compiled.js"
            nonCompiledJavascriptSrc="/static/scripts/app/matrix-list.js"/>
    <script type="text/javascript">
        //<![CDATA[
        indeed.proctor.app.matrix.list.start(${testMatrixDefinition});
        //]]>
    </script>
</layout:base>
