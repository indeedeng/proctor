<%@ tag language="java" pageEncoding="UTF-8" description="Pager interface including prev, next button and page number" body-content="scriptless" trimDirectiveWhitespaces="true" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ attribute name="extraCssClass" required="false" type="java.lang.String" rtexprvalue="true" %>
<div class="pager-controller ${extraCssClass}" style="display:none">
    <a class="pager-prev tiny button secondary radius" href="javascript:void(0)">Prev</a>
    <div class="pager-controller-text">
        <span class="pager-current-page">-</span> / <span class="pager-page-num">-</span>
    </div>
    <a class="pager-next tiny button secondary radius" href="javascript:void(0)">Next</a>
</div>