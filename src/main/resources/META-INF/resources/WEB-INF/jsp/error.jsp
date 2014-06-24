<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<html>
    <head>
        <title>Proctor - error</title>
    </head>
    <body>
        <c:out value="${fn:escapeXml(error)}" />
        <pre>
<c:out value="${exception}" />
        </pre>
    </body>
</html>
