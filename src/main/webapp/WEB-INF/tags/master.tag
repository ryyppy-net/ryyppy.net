<%@tag description="Master page" pageEncoding="UTF-8"%>
<%@taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
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
        <meta name="viewport" content="initial-scale=1.0, maximum-scale=1.0, user-scalable=0;">
        
        <title><c:out value="${title}"/></title>

        <link rel="stylesheet" type="text/css" href="/static/css/style.css" />
        <script type="text/javascript" src="/static/js/jquery.js"></script>

        <script type="text/javascript">
            var _gaq = _gaq || [];
            _gaq.push(['_setAccount', 'UA-22483744-1']);
            _gaq.push(['_trackPageview']);

            (function() {
                var ga = document.createElement('script'); ga.type = 'text/javascript'; ga.async = true;
                ga.src = ('https:' == document.location.protocol ? 'https://ssl' : 'http://www') + '.google-analytics.com/ga.js';
                var s = document.getElementsByTagName('script')[0]; s.parentNode.insertBefore(ga, s);
            })();
        </script>

        <jsp:invoke fragment="customHead" />
    </head>
    <body>
        <jsp:doBody/>
    </body>
</html>