<%@ tag import="com.indeed.proctor.webapp.extensions.renderer.DefinitionDeletePageRenderer" %>
<%@ tag language="java" pageEncoding="UTF-8" description="Popup view of a definition" body-content="scriptless" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="proctor" uri="http://tags.indeed.com/proctor" %>
<%@ taglib prefix="ui" tagdir="/WEB-INF/tags/ui" %>
<%@ attribute name="testName" type="java.lang.String" description="Test Name" %>
<%@ attribute name="src" type="java.lang.String" description="Source branch name" %>
<%@ attribute name="srcRevision" type="java.lang.String" description="Source revision number" %>
<form class="js-delete-definition" action="/proctor/definition/${testName}/delete" method="POST">
    <input type="hidden" value="${src}" name="src"/>
    <input type="hidden" value="${srcRevision}" name="srcRevision"/>
    <div class="panel js-save-info">
        <ui:grid-row>
            <ui:grid-columns width="three"><label class="right inline">SVN</label></ui:grid-columns>
            <ui:grid-columns width="four"><input placeholder="Username" type="text" name="username" /></ui:grid-columns>
            <ui:grid-columns width="five"><input placeholder="Password" type="password" name="password" /></ui:grid-columns>
        </ui:grid-row>
        <proctor:renderDefinitionDeletePageInjectionTemplates position="<%=DefinitionDeletePageRenderer.DefinitionDeletePagePosition.BOTTOM_FORM%>" testName="${testName}"/>
        <ui:grid-row>
            <ui:grid-columns width="three"><label class="right inline">Comment</label></ui:grid-columns>
            <ui:grid-columns width="nine"><input placeholder="Description of change" type="text" name="comment" /></ui:grid-columns>
        </ui:grid-row>
        <div class="ui-form-buttons">
            <input type="submit" name="delete-button" class="button js-save-form" value="Delete">
            <span class="button tiny secondary js-clean-workspace">clean workspace</span>
            <div style="display:none;" class="mam save-msg-container alert-box"></div>
        </div>
    </div>
    <proctor:renderDefinitionDeletePageInjectionTemplates position="<%=DefinitionDeletePageRenderer.DefinitionDeletePagePosition.SCRIPT%>" testName="${testName}"/>

</form>