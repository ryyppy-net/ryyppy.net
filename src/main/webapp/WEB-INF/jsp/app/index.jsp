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
        Spike: pre-populate $templateCache for the default route's templates so
        Angular doesn't have to fetch them over XHR after bootstrap. Normally
        $http (and therefore ngRoute's templateUrl / ng-include) checks this
        cache before issuing a network request; a script[type=text/ng-template]
        tag with id=<templateUrl> is Angular's built-in way to seed it from
        static HTML shipped with the page. This duplicates the content of
        app/partials/user.html, user_menu.html and user_button.html below -
        that duplication is a known, temporary tradeoff pending a follow-up fix.
    --%>
    <script type="text/ng-template" id="partials/user.html">
        <div ng-include="'partials/user_menu.html'"></div>

        <div class="container-fluid">
            <div class="row-fluid">
                <div class="span10 offset1">
                    <div ng-repeat="participant in participants" ng-include="'partials/user_button.html'"></div>

                    <h2>Bileesi</h2>

                    <div ng-show="parties.length <= 0">
                        <p>
                            Et näytä olevan vielä missään bileissä. Voit lisätä uudet bileet
                            <a href="#/party-admin">bileiden hallinnassa</a> tai antaa rekisteröityessäsi käyttämäsi
                            sähköpostiosoitteen kavereillesi, jotta he voivat kutsua sinut bileisiinsä.
                        </p>
                    </div>
                    <div ng-show="parties.length > 0">
                        <p>
                            Jos haluat muokata bileitäsi, voit tehdä sen <a href="#/party-admin">bileiden hallinnassa</a>.
                        </p>

                        <div ng-repeat="party in parties | orderBy:partySort:true">
                            <a href="#/party/{{ party.id }}">
                                <div class="party color{{ ($index % 12) + 1 }}">
                                    <div>{{ party.name }} <small style="float: right;"><span class="hidden-phone">Alkamisaika:</span> {{ party.startTime | formatDateTime }}</small></div>
                                    <ul class="inline" ng-show="party.participants.length > 0" style="margin-top: 5px; margin-bottom: 0px;">
                                        <li ng-repeat="participantPreview in party.participants" participant-preview="participantPreview">
                                        </li>
                                    </ul>
                                </div>
                            </a>
                        </div>
                    </div>

                    <h2>Historia</h2>

                    <div class="historyGraphContainer">
                        <div id="historyGraph">
                        </div>
                    </div>

                    <h2>5 viimeisintä juomaa</h2>

                    <div class="row-fluid" ng-repeat="drink in drinks | orderBy:drinkSort:true | limitTo:5" style="margin-top: 10px;">
                        <div class="span11">{{ drink.timestamp | formatISODateTime }} (Annoksia: {{ drink.amountOfShots | roundAmountOfShots }})</div>
                        <div class="span1"><button ng-click="removeDrink(drink)" class="btn btn-danger">Poista</button></div>
                    </div>
                </div>
            </div>
        </div>
    </script>

    <script type="text/ng-template" id="partials/user_menu.html">
        <div class="navbar hidden-phone">
            <div class="navbar-inner">
                <span class="brand">Ryyppy.net</span>
                <ul class="nav">
                    <li ng-class="active == 'user' && 'active'"><a href="#">Sinä</a></li>
                    <li ng-class="active == 'general-party-admin' && 'active'"><a href="#/party-admin">Bileiden hallinta</a></li>
                    <li ng-class="active == 'profile-settings' && 'active'"><a href="#/profile-settings">Asetukset</a></li>
                </ul>
                <ul class="nav pull-right">
                    <li><a href="/ui/user" title="Vaihda vanhaan käyttöliittymään">Vanha käyttöliittymä</a></li>
                    <li><a href="/logout" class="g_id_signout">Kirjaudu ulos</a></li>
                </ul>
            </div>
        </div>

        <div class="mobile-navbar navbar visible-phone">
            <div class="navbar-inner">
                <ul class="nav">
                    <li ng-class="active == 'user' && 'active'"><a href="#"><i class="icon-home"></i></a></li>
                    <li ng-class="active == 'general-party-admin' && 'active'"><a href="#/party-admin"><i class="icon-group"></i></a></li>
                    <li ng-class="active == 'profile-settings' && 'active'"><a href="#/profile-settings"><i class="icon-cog"></i></a></li>
                </ul>
                <ul class="nav mobile-nav pull-right">
                    <li><a href="/ui/user" title="Vaihda vanhaan käyttöliittymään"><i class="icon-share"></i></a></li>
                    <li><a href="/logout" class="g_id_signout"><i class="icon-signout"></i></a></li>
                </ul>
            </div>
        </div>
    </script>

    <script type="text/ng-template" id="partials/user_button.html">
        <div ng-controller="DrinkerCtrl">
            <div class="drinker color{{ participant.color }}" style="position: relative;">
                <div ng-click="addDefaultDrink(participant)" class="container-fluid" style="margin-top: 10px;">
                    <div class="row-fluid">
                        <div class="span4" style="text-align: center;">
                            <img ng-src="{{ participant.profilePictureUrl }}" />
                            <p>{{ participant.name }}</p>
                        </div>

                        <div class="span8">
                            <div class="row-fluid">
                                <div class="span12">
                                    <div id="graph{{ participant.id }}" style="height: 100px;"></div>
                                </div>
                            </div>

                            <div class="row-fluid">
                                <div class="span12" style="text-align: center;">
                                    <p>Juomia: {{ participant.totalDrinks }} kpl</p>
                                    <p>Promilleja:  {{ participant.promilles | number: 2 }}&permil;</p>
                                </div>
                            </div>
                        </div>
                    </div>
                </div>

                <div ng-show="showDrinkDialog && addingDrink" class="drinker-overlay">
                    <p>Lisätään juomaa käyttäjälle {{ participant.name }}...</p>

                    <p>Juoman koko on {{ formattedAlcoholSize() }} ja vahvuus {{ formattedAlcoholPercentage() }}.</p>
                    <div class="progress" style="margin-left: 10px; margin-right: 10px;">
                        <div class="bar"></div>
                    </div>

                    <div class="btn" ng-click="editDrink()">Muokkaa</div>
                    <div class="btn btn-danger" ng-click="cancelDrink()">Peruuta</div>
                </div>

                <div ng-show="showDrinkDialog && editingDrink" class="drinker-overlay">
                    <p>Muokkaa käyttäjän {{ participant.name }} juomaa</p>

                    <form ng-submit="addEditedDrink()" class="edit-drink">
                        <div style="clear: both;">
                            <label for="portionSize" class="edit-drink-label">Annoskoko</label>
                            <select id="portionSize"
                                    ng-model="selectedPortionSize"
                                    ng-options="portionSize.value as portionSize.text for portionSize in portionSizes"
                                    class="edit-drink-input">
                            </select>
                        </div>

                        <div style="clear: both;">
                            <label for="portionAlcoholPercentage" class="edit-drink-label">Alkoholi-%</label>
                            <select id="portionAlcoholPercentage"
                                    ng-model="selectedAlcoholPercentage"
                                    ng-options="portionAlcoholPercentage.value as portionAlcoholPercentage.text for portionAlcoholPercentage in portionAlcoholPercentages"
                                    class="edit-drink-input">
                            </select>
                        </div>

                        <div style="clear: both">
                            <button type="submit" class="btn btn-success">Lisää</button>
                            <div class="btn btn-danger" ng-click="hideDialog()">Peruuta</div>
                        </div>
                    </form>
                </div>
            </div>
        </div>
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
