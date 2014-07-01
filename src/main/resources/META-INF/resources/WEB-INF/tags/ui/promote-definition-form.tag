<%@ tag import="com.indeed.proctor.webapp.extensions.renderer.DefinitionHistoryPageRenderer" %>
<%@ tag language="java" pageEncoding="UTF-8" description="Popup view of a definition" body-content="scriptless" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="proctor" uri="http://tags.indeed.com/proctor" %>
<%@ taglib prefix="ui" tagdir="/WEB-INF/tags/ui" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ attribute name="testName" type="java.lang.String" description="Test Name" %>
<%@ attribute name="src" type="java.lang.String" description="Source branch name" %>
<%@ attribute name="srcRevision" type="java.lang.String" description="Source revision number" %>
<%@ attribute name="dest" type="java.lang.String" description="Destination branch name" %>
<%@ attribute name="destRevision" type="java.lang.String" description="Destination revision number" %>
<%@ attribute name="promoteText" type="java.lang.String" description="Test Name" %>
<%@ attribute name="testDefinitionVersion" type="com.indeed.proctor.store.Revision" description="Test Definition Version" %>
<form class="js-promote-definition" action="/proctor/definition/${proctor:urlencode(testName)}/promote" method="POST">
    <input type="hidden" value="${fn:escapeXml(src)}" name="src"/>
    <input type="hidden" value="${srcRevision}" name="srcRevision"/>
    <input type="hidden" value="${fn:escapeXml(dest)}" name="dest"/>
    <input type="hidden" value="${destRevision}" name="destRevision"/>
    <div class="panel js-save-info">
        <ui:grid-row>
            <ui:grid-columns width="three"><label class="right inline">SVN</label></ui:grid-columns>
            <ui:grid-columns width="four"><input placeholder="Username" type="text" name="username" /></ui:grid-columns>
            <ui:grid-columns width="five"><input placeholder="Password" type="password" name="password" /></ui:grid-columns>
        </ui:grid-row>
        <proctor:renderDefinitionHistoryPageInjectionTemplates position="<%=DefinitionHistoryPageRenderer.DefinitionHistoryPagePosition.PROMOTE_FORM_BOTTOM%>" testName="${testName}" testDefinitionVersion="${testDefinitionVersion}"/>
        <div class="ui-form-buttons">
            <input type="submit" class="button js-save-form" value="${fn:escapeXml(promoteText)}">
            <span class="button tiny secondary js-clean-workspace">clean workspace</span>
            <div style="display:none;" class="mam save-msg-container alert-box"></div>
        </div>
    </div>
</form>
