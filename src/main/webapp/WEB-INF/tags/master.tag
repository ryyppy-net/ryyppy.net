<%@tag description="Master page" pageEncoding="UTF-8"%>
<%@taglib uri="jakarta.tags.core" prefix="c" %>
<%@taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<%@attribute name="customHead" fragment="true" %>
<%@attribute name="title" rtexprvalue="true" %>

<c:if test="${empty title}" >
    <c:set var="title" value="Ryyppy.net" />
</c:if>

<!DOCTYPE html>
<!-- Ryyppy.net versio: <fmt:message key="application.version" /> -->
<html>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
        <meta name="viewport" content="initial-scale=1.0, maximum-scale=1.0, user-scalable=0">

        <title><c:out value="${title}"/></title>

        <script type="text/javascript" src="/static/js/jquery.js"></script>
        <script type="text/javascript" src="/static/js/jquery.tooltip.min.js"></script>
        <link href="https://fonts.googleapis.com/css?family=Rum+Raisin&subset=latin,latin-ext" rel="stylesheet" type="text/css" />

        <jsp:invoke fragment="customHead" />
    </head>
    <body>
        <jsp:doBody/>
    </body>
</html>