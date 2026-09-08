<%@page contentType="text/html" pageEncoding="UTF-8" isELIgnored="false"%>
<%@taglib uri="jakarta.tags.core" prefix="c" %>
<%@taglib uri="jakarta.tags.functions" prefix="fn" %>
<!doctype html>
<html lang="en" ng-app="ryyppy">
<head>
    <meta charset="utf-8">
    <title>Ryyppy.net</title>

    <link rel="stylesheet" href="/static/vendor/bootstrap/css/bootstrap.min.css"/>
    <link rel="stylesheet" href="/static/vendor/font-awesome/font-awesome.css"/>
    <link rel="stylesheet" href="/webjars/pnotify/1.2.0/jquery.pnotify.default.css"/>
    <link rel="stylesheet" href="<c:url value="/app/css/app.css"/>"/>

    <!-- media="print" + onload swap keeps this from blocking the synchronous
         <script> tags below on a slow/unreachable Google Fonts request. -->
    <link href='https://fonts.googleapis.com/css?family=Rum+Raisin&subset=latin,latin-ext' rel='stylesheet' type='text/css' media="print" onload="this.media='all'">
    <noscript><link href='https://fonts.googleapis.com/css?family=Rum+Raisin&subset=latin,latin-ext' rel='stylesheet' type='text/css'></noscript>

    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <script src="https://accounts.google.com/gsi/client" async defer></script>
</head>
<body>
    <div ng-view></div>

    <%--
        Pre-populate $templateCache with every route's template (loaded from
        app/partials/ by DefaultController.appIndex()) so Angular never
        fetches one over XHR. ${...} is intentionally unescaped: trusted
        server-side markup, not user input.
    --%>
    <c:forEach var="template" items="${templates}">
        <script type="text/ng-template" id="${template.key}">${template.value}</script>
    </c:forEach>

    <%--
        Embed the current user's own profile (DefaultController.appIndex(),
        same JSON shape as GET /API/v2/profile) so UserCtrl can skip its first
        fetch on load. Unlike the trusted-markup templates above, this JSON can
        contain user-controlled strings (name, email), so it's HTML-escaped
        with fn:escapeXml and re-parsed at runtime rather than embedded as raw
        EL - that avoids both a premature </script> close and HTML injection.
    --%>
    <script>
        window.__INITIAL_PROFILE__ = JSON.parse('${fn:escapeXml(initialProfile)}');
    </script>

    <script src="/static/vendor/angular/angular.min.js"></script>
    <script src="<c:url value="/app/js/app.js"/>"></script>
    <script src="<c:url value="/app/js/services.js"/>"></script>

    <script src="/static/vendor/moment/moment.min.js"></script>

    <script src="<c:url value="/app/js/controllers/UserCtrl.js"/>"></script>
    <script src="<c:url value="/app/js/controllers/ProfileSettingsCtrl.js"/>"></script>
    <script src="<c:url value="/app/js/controllers/DrinkerCtrl.js"/>"></script>
    <script src="<c:url value="/app/js/controllers/PartyCtrl.js"/>"></script>
    <script src="<c:url value="/app/js/controllers/GeneralPartyAdminCtrl.js"/>"></script>
    <script src="<c:url value="/app/js/controllers/PartyAdminCtrl.js"/>"></script>

    <script src="<c:url value="/app/js/userhistorygraph.js"/>"></script>

    <script src="<c:url value="/app/js/filters.js"/>"></script>
    <script src="<c:url value="/app/js/directives.js"/>"></script>

    <script src="/webjars/jquery/1.8.3/jquery.min.js"></script>
    <script src="/webjars/pnotify/1.2.0/jquery.pnotify.js"></script>

    <script type="text/javascript" src="/webjars/flot/0.7/jquery.flot.min.js"></script>
    <script type="text/javascript" src="/webjars/flot/0.7/jquery.flot.crosshair.min.js"></script>
    <script type="text/javascript" src="/webjars/flot/0.7/jquery.flot.resize.min.js"></script>
</body>
</html>
