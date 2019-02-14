<%@ tag language="java" pageEncoding="UTF-8" description="Simple 2 columns grid-row" body-content="scriptless" trimDirectiveWhitespaces="true" %>
<%@ attribute name="width" required="true" type="java.lang.String" rtexprvalue="true" %>
<%@ attribute name="extraCssClass" required="false" type="java.lang.String" rtexprvalue="true" %>
<div class="${width} columns ${extraCssClass}">
<jsp:doBody />
</div>
