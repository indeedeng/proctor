<%@ tag language="java" pageEncoding="UTF-8" description="Simple 2 columns grid-row" body-content="scriptless" trimDirectiveWhitespaces="true" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ attribute name="collapse" required="false" type="java.lang.Boolean" rtexprvalue="true" %>
<%@ attribute name="extraCssClass" required="false" type="java.lang.String" rtexprvalue="true" %>
<div class="row ${extraCssClass} <c:if test="${collapse}">collapse</c:if>">
<jsp:doBody />
</div>
