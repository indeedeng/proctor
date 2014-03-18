<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="proctor" uri="http://tags.indeed.com/proctor" %>
<%@ taglib prefix="ui" tagdir="/WEB-INF/tags/ui" %>
<%@ tag language="java" pageEncoding="UTF-8" description="Popup view of a definition" body-content="scriptless" %>
<%@ attribute name="testName" type="java.lang.String" description="Test Name" %>
<%@ attribute name="branch" type="com.indeed.proctor.webapp.db.Environment" description="Branch" %>
<%@ attribute name="testDefinitionHistory" type="java.util.List" description="java.util.List<com.indeed.proctor.store.Revision>" %>
<%@ attribute name="version" type="com.indeed.proctor.common.EnvironmentVersion" description="Versions across different branches" %>
<c:forEach items="${testDefinitionHistory}" var="testDefinitionVersion">
    <c:set var="isTrunkRevision" value="${proctor:isCurrentVersionOnTrunk(branch, testDefinitionVersion, version)}" />
    <c:set var="isQaRevision" value="${proctor:isCurrentVersionOnQa(branch, testDefinitionVersion, version)}" />
    <c:set var="isProductionRevision" value="${proctor:isCurrentVersionOnProduction(branch, testDefinitionVersion, version)}" />

    <div class="ui-def-hist-commit pbm mbm <c:if test="${isTrunkRevision}">ui-def-hist-trunk </c:if><c:if test="${isQaRevision}">ui-def-hist-qa </c:if><c:if test="${isProductionRevision}">ui-def-hist-production </c:if>">
        <c:if test="${isTrunkRevision || isQaRevision || isProductionRevision}">
            <div class="ui-tagbar ui-tagbar-right">
                <c:if test="${isTrunkRevision}"><a class="mlm radius label" href="/proctor/definition/${testName}">TRUNK r${version.trunkVersion}</a></c:if>
                <c:if test="${isQaRevision}"><a class="mlm radius label" href="/proctor/definition/${testName}?branch=qa">QA r${version.qaVersion}</a></c:if>
                <c:if test="${isProductionRevision}"><a class="mlm radius label" href="/proctor/definition/${testName}?branch=production">PRODUCTION r${version.productionVersion}</a></c:if>
            </div>
        </c:if>
        <span><proctor:formatRevisionDisplay revision="${testDefinitionVersion}"/></span>
        <pre><proctor:formatCommitMessageDisplay commitMessage="${testDefinitionVersion.message}"/></pre>
        <div>
            <c:if test="${!isQaRevision && branch.name == 'trunk'}">
            <ui:expand-collapse more="Promote r${testDefinitionVersion.revision} to QA" less="Cancel" isMoreExpanded="false" >
                <%-- TODO: parker 2012-09-04 Depending on the current branch (eg displaying history of QA, you cannot promote to QA) --%>
                <ui:promote-definition-form testName="${testName}"
                                            src="${branch.name}"
                                            srcRevision="${testDefinitionVersion.revision}"
                                            dest="qa"
                                            destRevision="${version.qaRevision}"
                                            promoteText="Promote r${testDefinitionVersion.revision} to QA"
                                            testDefinitionVersion="${testDefinitionVersion}"
                />
            </ui:expand-collapse>
            </c:if>
            <c:if test="${!isProductionRevision && (branch.name == 'trunk' || branch.name == 'qa')}">
            <ui:expand-collapse more="Promote r${testDefinitionVersion.revision} to Production" less="Cancel" isMoreExpanded="false">
                <ui:promote-definition-form testName="${testName}"
                                            src="${branch.name}"
                                            srcRevision="${testDefinitionVersion.revision}"
                                            dest="production"
                                            destRevision="${version.productionRevision}"
                                            promoteText="Promote r${testDefinitionVersion.revision} to Production"
                                            testDefinitionVersion="${testDefinitionVersion}"
                />
            </ui:expand-collapse>
            </c:if>
        </div>
    </div>
</c:forEach>
