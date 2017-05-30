<%@ tag language="java" pageEncoding="UTF-8" description="Pager interface including prev, next button and page number" body-content="scriptless" trimDirectiveWhitespaces="true" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<div class="pager-controller">
    <a class="pager-prev tiny button secondary radius" href="javascript:void(0)">Prev</a>
    <span class="pager-current-page">-</span> / <span class="pager-page-num">-</span>
    <a class="pager-next tiny button secondary radius" href="javascript:void(0)">Next</a>
</div>