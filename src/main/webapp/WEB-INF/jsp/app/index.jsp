<%@page contentType="text/html" pageEncoding="UTF-8" isELIgnored="false"%>
<%@taglib uri="jakarta.tags.core" prefix="c" %>
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
        Pre-populate $templateCache for the default route's templates so
        Angular doesn't have to fetch them over XHR after bootstrap. Normally
        $http (and therefore ngRoute's templateUrl / ng-include) checks this
        cache before issuing a network request; a script[type=text/ng-template]
        tag with id=<templateUrl> is Angular's built-in way to seed it from
        static HTML shipped with the page. DefaultController.appIndex() reads
        each file straight from app/partials/ on the classpath into the model,
        so there's no separate copy to drift out of sync with the originals.
        ${...} is deliberately unescaped here (JSP EL doesn't HTML-escape by
        default) - this is trusted server-side markup, not user input.
    --%>
    <script type="text/ng-template" id="partials/user.html">${userTemplate}</script>
    <script type="text/ng-template" id="partials/user_menu.html">${userMenuTemplate}</script>
    <script type="text/ng-template" id="partials/user_button.html">${userButtonTemplate}</script>

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
