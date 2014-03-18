<%@ tag language="java" pageEncoding="UTF-8" description="Popup view of a definition" body-content="scriptless" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="proctor" uri="http://tags.indeed.com/proctor" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ attribute name="definition" required="true" type="com.indeed.proctor.common.model.TestDefinition" %>
<ul class="ui-bucket-index no-bullet">
<c:forEach items="${definition.buckets}" var="bucket">
    <li class="media"><span class="mas img ui-color-swatch ui-color${(1+bucket.value) % 12}"></span><span class="bd ui-bucket-index-label">${fn:escapeXml(bucket.name)}<c:if test="${!empty bucket.description}"> - ${fn:escapeXml(bucket.description)}</c:if></span></li>
</c:forEach>
</ul>
