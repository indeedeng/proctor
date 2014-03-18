<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="ui" tagdir="/WEB-INF/tags/ui" %>
<%@ taglib prefix="layout" tagdir="/WEB-INF/tags/layout" %>
<%@ taglib prefix="proctor" uri="http://tags.indeed.com/proctor" %>
<%--@elvariable id="session" type="com.indeed.proctor.webapp.model.SessionViewModel"--%>
<%--@elvariable id="jobs" type="java.util.List<com.indeed.proctor.webapp.controllers.BackgroundJob>"--%>

<layout:base title="Jobs" session="${session}" >

    <ui:expand-collapse more="Clean User Workspace" less="nevermind" isMoreExpanded="false" >
        <ui:clean-workspace-form />
    </ui:expand-collapse>
    <table class="w100">
        <thead>
            <tr><th>JobId</th><th>Status</th><th>Description</th></tr>
        </thead>
        <tbody>
        <c:forEach items="${jobs}" var="job" varStatus="loopStatus">
            <tr data-jobid="${job.id}">
                <td>${job.id}</td>
                <td><a href="#${job.id}" class="tiny button js-job-view"><c:choose><c:when test="${job.running}">RUNNING</c:when><c:otherwise>${job.status}</c:otherwise></c:choose></a></td>
                <td>${job.title}</td>
            </tr>
        </c:forEach>

        </tbody>
    </table>

    <layout:javascript
            useCompiledJavascript="${session.useCompiledJavaScript}"
            compiledJavascriptSrc="/static/scripts/app/matrix-list-compiled.js"
            nonCompiledJavascriptSrc="/static/scripts/app/matrix-list.js"/>
    <script type="text/javascript">
        //<![CDATA[
        indeed.proctor.app.jobs.start();
        //]]>
    </script>

</layout:base>
