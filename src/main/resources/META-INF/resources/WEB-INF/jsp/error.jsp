<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<html>
    <head>
        <title>Proctor - error</title>
    </head>
    <body>
        <c:out value="${error}" />
        <pre>
<c:out value="${exception}" />
        </pre>
    </body>
</html>
