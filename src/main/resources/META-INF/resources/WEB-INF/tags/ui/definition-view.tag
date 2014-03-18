<%@ tag import="com.indeed.proctor.webapp.extensions.renderer.DefinitionDetailsPageRenderer" %>
<%@ tag language="java" pageEncoding="UTF-8" description="Popup view of a definition" body-content="scriptless" %>
<%@ taglib prefix="ui" tagdir="/WEB-INF/tags/ui" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="proctor" uri="http://tags.indeed.com/proctor" %>
<%@ attribute name="testName" required="true" type="java.lang.String" %>
<%@ attribute name="definition" required="true" type="com.indeed.proctor.common.model.TestDefinition" %>
<div class="ui-test-definition ui-popover mod collapsible">
    <div class="inner">
        <div class="hd">
            <div class="media">
                <div><span class="field-label">Description:</span>${definition.description}</div>
                <div><span class="field-label">Type:</span>${definition.testType}</div>
                <div><span class="field-label">version:</span>${definition.version}</div>
                <div><span class="field-label">rule:</span>${definition.rule}</div>
                <div><span class="field-label">salt:</span>${definition.salt}</div>
                <proctor:renderDefinitionDetailsPageInjectionTemplates position="<%=DefinitionDetailsPageRenderer.DefinitionDetailsPagePosition.TOP%>" testName="${testName}" testDefinition="${definition}"/>
            </div>
            <%--
            <div class="row">
                <div class="three columns">
                    <h5>${testName}</h5>
                </div>
                <div class="nine columns">
                    <div class="row"><ui:grid-columns width="nine"><span class="field-label">Description:</span>${definition.description}</ui:grid-columns></div>
                    <div class="row">
                        <div class="two columns"><span class="field-label">ver:</span>${definition.version}</div>
                        <div class="four columns"><span class="field-label">rule:</span>${definition.rule}</div>
                        <div class="three columns"><span class="field-label">salt:</span>${definition.salt}</div>
                    </div>
                </div>
            </div>
            --%>
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
