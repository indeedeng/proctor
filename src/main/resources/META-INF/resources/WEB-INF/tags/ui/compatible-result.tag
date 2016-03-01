<%@ tag language="java" display-name="compatible-result" pageEncoding="UTF-8" body-content="scriptless" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="proctor" uri="http://tags.indeed.com/proctor" %>
<%@ taglib prefix="ui" tagdir="/WEB-INF/tags/ui" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ attribute name="version" required="true" type="java.lang.Object" description="com.indeed.proctor.webapp.controllers.ProctorController$CompatibleSpecificationResult" %>
<%@ attribute name="branch" required="true" type="java.lang.String" %>
<c:if test="${version.compatible}">
    <span class="label success">${fn:escapeXml(version.toShortString())}</span><a class="ui-icon" href="/proctor/specification?branch=${proctor:urlencode(branch)}&version=${proctor:urlencode(version.appVersion.version)}&app=${proctor:urlencode(version.appVersion.app)}">sp</a>
</c:if>
<c:if test="${!version.compatible}">
    <div class="ui-expand-collapse ui-collapsed">
        <a href="javascript:void(0)" class="ui-expand-title ui-expand"><span class="label alert">${fn:escapeXml(version.toShortString())}</span></a>
        <a href="javascript:void(0)" class="ui-expand-title ui-collapse"><span class="label alert">${fn:escapeXml(version.toShortString())}</span></a>
        <a class="ui-icon" href="/proctor/specification?branch=${proctor:urlencode(branch)}&version=${proctor:urlencode(version.appVersion.version)}&app=${proctor:urlencode(version.appVersion.app)}">sp</a>
        <div class="ui-expand-collapse-bd">${fn:escapeXml(version.error)}</div>
    </div>
</c:if>
