<%@ tag language="java" pageEncoding="UTF-8" description="Pager interface including message box" body-content="scriptless" trimDirectiveWhitespaces="true" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ attribute name="extraCssClass" required="false" type="java.lang.String" rtexprvalue="true" %>
<div class="pager-messenger ${extraCssClass}">
    <span class="pager-message-no-result" style="display:none;">No results found for the search criteria.</span>
</div>