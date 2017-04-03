<%@ tag language="java" pageEncoding="UTF-8" body-content="scriptless" trimDirectiveWhitespaces="true" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="proctor" uri="http://tags.indeed.com/proctor" %>
<%@ taglib prefix="ui" tagdir="/WEB-INF/tags/ui" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ attribute name="constants" required="true" type="java.util.Map" %>
<%@ attribute name="inputPath" required="true" type="java.lang.String" %>
<div class="js-constants-editor">
    <ui:grid-row >
        <ui:grid-columns width="six"><h6>Variable</h6></ui:grid-columns>
        <ui:grid-columns width="six"><h6>Value</h6></ui:grid-columns>
    </ui:grid-row>
    <div class="js-constants">
        <c:forEach items="${constants}" var="constant" varStatus="status">
            <ui:grid-row extraCssClass="ui-constant-row js-constant-row">
                <ui:grid-columns width="six"><label class="inline js-constant-variable">${fn:escapeXml(constant.key)}</label></ui:grid-columns>
                <ui:grid-columns width="six"><label class="inline">${fn:escapeXml(constant.value)}</label></ui:grid-columns>
            </ui:grid-row>
        </c:forEach>
     </div>
    <div>
        <textarea rows="4" cols="4" class="ui-pretty-json js-edit-constants json" name="${fn:escapeXml(inputPath)}" data-json-type="raw">${fn:escapeXml(proctor:prettyPrintJSON(constants))}</textarea>
        <div style="display:none;" class="constants-msg-container alert-box"></div>
    </div>
</div>
