<%@ tag language="java" pageEncoding="UTF-8" description="Popup view of a definition" body-content="scriptless" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ attribute name="definition" required="true" type="com.indeed.proctor.common.model.TestDefinition" %>
<%@ attribute name="selectedBucketValue" required="true" type="java.lang.Integer" %>
<%@ attribute name="inputName" required="true" type="java.lang.String" %>
<%@ attribute name="extraCssClass" required="false" type="java.lang.String" rtexprvalue="true" %>

<select name="${inputName}" class="js-bucket-select ${extraCssClass}">
<c:forEach items="${definition.buckets}" var="bucket">
    <option value="${bucket.value}" <c:if test="${bucket.value == selectedBucketValue}">selected="selected"</c:if> >${fn:escapeXml(bucket.name)}</option>
</c:forEach>
</select>
