<%@ tag import="com.indeed.proctor.webapp.extensions.renderer.BasePageRenderer" %>
<%@ tag display-name="base" description="Standard template for proctor pages" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="ui" tagdir="/WEB-INF/tags/ui" %>
<%@ taglib prefix="proctor" uri="http://tags.indeed.com/proctor" %>

<%-- TODO: baseview model--%>
<%@ attribute name="title" required="false" type="java.lang.String" %>
<%@ attribute name="canonicalLink" required="false" type="java.lang.String" %>
<%@ attribute name="session" required="false" type="com.indeed.proctor.webapp.model.SessionViewModel" %>
<%@ attribute name="branch" required="false" type="com.indeed.proctor.webapp.db.Environment" %>
<%@ attribute name="emptyClients" required="false" type="java.lang.Boolean" %>
<!DOCTYPE html>

<!-- paulirish.com/2008/conditional-stylesheets-vs-css-hacks-answer-neither/ -->
<!--[if lt IE 7]> <html class="lt-ie9 lt-ie8 lt-ie7" lang="en"> <![endif]-->
<!--[if IE 7]>    <html class="lt-ie9 lt-ie8" lang="en"> <![endif]-->
<!--[if IE 8]>    <html class="lt-ie9" lang="en"> <![endif]-->
<!--[if gt IE 8]><!--> <html class="" lang="en"> <!--<![endif]-->
<head>
  <title>${not empty title ? fn:escapeXml(title): 'Proctor' }</title>
  <meta charset="utf-8" />

  <!-- Set the viewport width to device width for mobile -->
  <meta name="viewport" content="width=device-width" />

  <!-- Included CSS Files -->
    <c:choose>
        <c:when test="${session.useCompiledCSS}">
            <link rel="stylesheet" href="/static/styles/styles-compiled.css">
        </c:when>
        <c:otherwise>
            <link rel="stylesheet" href="/static/lib/normalize/normalize.css">
            <link rel="stylesheet" href="/static/lib/foundation/stylesheets/foundation.css">
            <link rel="stylesheet" href="/static/lib/oocss/mod.css">
            <link rel="stylesheet" href="/static/lib/oocss/media.css">
            <link rel="stylesheet" href="/static/lib/oocss/space.css">
            <link rel="stylesheet" href="/static/styles/app.css">
        </c:otherwise>
    </c:choose>

  <c:if test="${not empty canonicalLink}"><link rel="canonical" href="${canonicalLink}" /></c:if>
</head>
<body>
    <div class="page-container">
        <div class="hd">
            <ul class="nav-bar">
                <li ><a href="/proctor/">Test Matrix</a></li>
                <c:if test="${empty emptyClients || ! emptyClients}"> <li ><a href="/proctor/usage">Usage</a></li> </c:if>
                <c:if test="${empty emptyClients || ! emptyClients}"> <li ><a href="/proctor/compatibility">Compatibility</a></li> </c:if>
                <li><a href="/proctor/definition/create">Create new test</a></li>
                <proctor:renderBasePageInjectionTemplates position="<%=BasePageRenderer.BasePagePosition.NAVBAR_BUTTON%>" branch="${branch}"/>
                <li class="nav-bar-right">
                    <dl class="sub-nav">
                        <dt>Branch:</dt>
                        <dd <c:if test="${empty branch || branch.name == 'trunk'}"> class="active"</c:if> ><a href="${requestScope["javax.servlet.forward.request_uri"]}?branch=trunk"/>TRUNK</a></dd>
                        <dd <c:if test="${not empty branch && branch.name == 'qa'}"> class="active"</c:if> ><a href="${requestScope["javax.servlet.forward.request_uri"]}?branch=qa"/>QA</a></dd>
                        <dd <c:if test="${not empty branch && branch.name == 'production'}"> class="active"</c:if> ><a href="${requestScope["javax.servlet.forward.request_uri"]}?branch=production"/>PRODUCTION</a></dd>
                    </dl>
                </li>
            </ul>
        </div>
        <div class="inner row">
            <div class="bd">
                <jsp:doBody/>
            </div>

            <div class="ft">
            </div>
        </div>
    </div>
    <proctor:renderBasePageInjectionTemplates position="<%=BasePageRenderer.BasePagePosition.FOOTER%>" branch="${branch}"/>
</body>
</html>
