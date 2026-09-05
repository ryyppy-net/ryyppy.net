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

        <script type="text/javascript" src="/webjars/jquery/1.8.3/jquery.min.js"></script>
        <%-- jQuery UI 1.8.12 (bundled below on pages that need it) calls the
             internal $.curCSS helper, which jQuery removed in 1.8.0. Without
             this shim, that throws "curCSS is not a function" partway through
             widget setup, silently aborting init calls like
             $(dialog).dialog({autoOpen: false}) and leaving the dialog
             visible instead of hidden. --%>
        <script type="text/javascript">
            jQuery.curCSS = jQuery.curCSS || jQuery.css;
        </script>
        <script type="text/javascript" src="/static/vendor/jquery-tooltip/jquery.tooltip.min.js"></script>
        <script src="https://accounts.google.com/gsi/client" async defer></script>
        <%-- Loaded non-render-blocking: a pending <link rel="stylesheet"> in <head>
             delays every synchronous <script> that follows it (and the scripts
             customHead adds), so a slow/unreachable Google Fonts request would
             otherwise stall the whole page. media="print" makes the browser fetch
             it without waiting on it, then the onload swap applies it once ready. --%>
        <link href="https://fonts.googleapis.com/css?family=Rum+Raisin&subset=latin,latin-ext" rel="stylesheet" type="text/css" media="print" onload="this.media='all'" />
        <noscript><link href="https://fonts.googleapis.com/css?family=Rum+Raisin&subset=latin,latin-ext" rel="stylesheet" type="text/css" /></noscript>

        <link rel="stylesheet" href="/static/css/style.css" type="text/css" media="screen" />
        <jsp:invoke fragment="customHead" />
    </head>
    <body>
        <jsp:doBody/>
    </body>
</html>