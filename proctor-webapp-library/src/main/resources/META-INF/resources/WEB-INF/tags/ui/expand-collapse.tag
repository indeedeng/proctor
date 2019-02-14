<%@ tag language="java" pageEncoding="UTF-8" description="Popup view of a definition" body-content="scriptless" trimDirectiveWhitespaces="true" %>
<%@ taglib prefix="ui" tagdir="/WEB-INF/tags/ui" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="proctor" uri="http://tags.indeed.com/proctor" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ attribute name="more" required="true" type="java.lang.String" %>
<%@ attribute name="less" required="true" type="java.lang.String" %>
<%@ attribute name="isMoreExpanded" required="true" type="java.lang.Boolean" %>
<div class="ui-expand-collapse <c:if test="${not isMoreExpanded}">ui-collapsed</c:if>">
  <div class="ui-expand-collapse-hdr"><a href="javascript:void(0)" class="ui-expand-title ui-expand">${fn:escapeXml(more)}</a><a href="javascript:void(0)" class="ui-expand-title ui-collapse">${fn:escapeXml(less)}</a></div>
  <div class="ui-expand-collapse-bd"><jsp:doBody /></div>
</div>
