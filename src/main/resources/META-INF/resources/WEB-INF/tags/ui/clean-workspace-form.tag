<%@ tag language="java" pageEncoding="UTF-8" description="Popup view of a definition" body-content="scriptless" trimDirectiveWhitespaces="true" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="proctor" uri="http://tags.indeed.com/proctor" %>
<%@ taglib prefix="ui" tagdir="/WEB-INF/tags/ui" %>
<form action="/proctor/rpc/svn/clean-working-directory" method="POST" target="_blank">
    <div class="panel">
        <ui:grid-row>
            <ui:grid-columns width="three"><label class="right inline">Username</label></ui:grid-columns>
            <ui:grid-columns width="three"><input placeholder="Username" type="text" name="username" /></ui:grid-columns>
            <ui:grid-columns width="six">
                <input type="submit" class="button js-clean-workspace" value="clean workspace">
            </ui:grid-columns>
        </ui:grid-row>
    </div>
</form>
