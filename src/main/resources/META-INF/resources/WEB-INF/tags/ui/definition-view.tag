<%@ tag import="com.indeed.proctor.webapp.extensions.renderer.DefinitionDetailsPageRenderer" %>
<%@ tag language="java" pageEncoding="UTF-8" description="Popup view of a definition" body-content="scriptless" trimDirectiveWhitespaces="true" %>
<%@ taglib prefix="ui" tagdir="/WEB-INF/tags/ui" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="proctor" uri="http://tags.indeed.com/proctor" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ attribute name="testName" required="true" type="java.lang.String" %>
<%@ attribute name="definition" required="true" type="com.indeed.proctor.common.model.TestDefinition" %>
<div class="ui-test-definition ui-popover mod collapsible">
    <div class="inner">
        <div class="hd">
            <div class="media">
                <div><span class="field-label">Description:</span><proctor:formatCommitMessageDisplay commitMessage="${definition.description}"/></div>
                <div><span class="field-label">Type:</span>${fn:escapeXml(definition.testType)}</div>
                <div><span class="field-label">version:</span>${definition.version}</div>
                <div><span class="field-label">rule:</span>${fn:escapeXml(definition.rule)}</div>
                <div><span class="field-label">salt:</span>${fn:escapeXml(definition.salt)}</div>
                <proctor:renderDefinitionDetailsPageInjectionTemplates position="<%=DefinitionDetailsPageRenderer.DefinitionDetailsPagePosition.TOP%>" testName="${testName}" testDefinition="${definition}"/>
            </div>
        </div>
        <div class="bd collapse-area ui-definition-bd">
            <c:if test="${!empty definition.constants}">
            <div class="media">
                <span class="img field-label">Constants</span>
                <div class="bd">
                    <ui:constants constants="${definition.constants}"/>
                </div>
            </div>
            </c:if>
            <c:if test="${!empty definition.specialConstants}">
            <div class="media">
                <span class="img field-label">Special Constants</span>
                <div class="bd">
                    <ui:constants constants="${definition.specialConstants}"/>
                </div>
            </div>
            </c:if>
            <div class="media">
                <span class="img field-label">Buckets</span>
                <div class="bd">
                    <ui:buckets definition="${definition}"/>
                </div>
            </div>
            <h6>Allocations</h6>
            <ui:allocations definition="${definition}"/>
        </div>
    </div>
</div>
