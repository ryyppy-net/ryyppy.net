<%@page contentType="text/html" pageEncoding="UTF-8" isELIgnored="false"%>
<%@taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@taglib prefix="t" tagdir="/WEB-INF/tags" %>
<t:master title="Bileet">
    <jsp:attribute name="customHead">
        <link rel="stylesheet" href="/static/css/jquery.tooltip.css" type="text/css" media="screen" />
        <link rel="stylesheet" href="/static/css/jquery-ui/jquery-ui-1.8.11.custom.css" type="text/css" media="screen" />
        <script type="text/javascript" src="/static/js/jquery.tooltip.min.js"></script>
        <script type="text/javascript" src="/static/js/common.js"></script>
        <script type="text/javascript" src="/static/js/jquery-ui-1.8.11.custom.min.js"></script>
        <script type="text/javascript" src="/static/js/jquery-ui-timepicker-addon.js"></script>
        <script type="text/javascript" src="/static/js/jquery.ui.datepicker-fi.js"></script>
        
        <!-- hack -->
        <!--[if lte IE 8]><script language="javascript" type="text/javascript" src="/static/js/flot/excanvas.min.js"></script><![endif]-->
        <script type="text/javascript" src="/static/js/flot/jquery.flot.min.js"></script>
        <script type="text/javascript" src="/static/js/flot/jquery.flot.crosshair.min.js"></script>
        <script type="text/javascript" src="/static/js/flot/jquery.flot.resize.min.js"></script>
        <script type="text/javascript" src="/static/js/userbutton.js"></script>
        <script type="text/javascript" src="/static/js/drinkerchecks.js"></script>
        <script type="text/javascript" src="/static/js/userhistorygraph.js"></script>
        <script type="text/javascript" src="/static/js/date.js"></script>
        <script type="text/javascript">
            var userButton = null;
            
            $(document).ready(function() {
                fixTheFuckingCss();

                $('.party').each(function(index) {
                    $(this).css('background-color', colors[index]);
                });
                
                userButton = new UserButton(<c:out value="${user.id}" />, $('.userButton'), getColorAtIndex(0));
                userButton.onDataLoaded = onButtonDataUpdated;
                userButton.update();
                setInterval(function() {userButton.update()}, 60 * 1000);

                var user = { id: ${user.id}, name: '${user.name}' };
                
                var element = $('#historyGraph');
                var graph = new UserHistoryGraph(user, element);
                graph.options.legend = { position:'nw' };
                graph.update();
            });

            function onButtonDataUpdated() {
                if (userButton.series == null) return;

                var max = 0;
                for (var j in userButton.series[0].data) {
                    var d = userButton.series[0].data[j];
                    var a = d[1];
                    if (Number(a) >= Number(max))
                        max = a;
                }
                max = Math.floor(max) + 1;
                userButton.setMaxY(max);
            }

            function configureDrinksDialogOpened() {
                $.get("/API/users/${user.id}/show-drinks", gotDrinkData);
                $("#time").datetimepicker($.datepicker.regional['fi']);
                $("#time").datetimepicker("option", {maxDate: 0});
                $("#time").datetimepicker("setDate", new Date());
            }

            function configureDrinksDialogClosed() {
                location.reload(true);
            }

            function formatDate(date) {
                return date.toString('yyyy-MM-dd HH:mm');
            }

            function gotDrinkData(data) {
                $("#drinksList").html("");
                var count = 0;
                $(data).find('drink').each(function() {
                    count++;
                    var li = $('<li>');
                    var id = $(this).find('id').text();
                    var timestamp = Number($(this).find('timestamp').text());

                    var date = formatDate(new Date(timestamp));

                    li.html('<a href="#" onclick="if (confirm(\'Haluatko varmasti poistaa juoman?\')) $.get(\'/ui/removeDrink?userId=' + ${user.id} + '&drinkId=' + id + '\', configureDrinksDialogOpened);">Juoma-aika: ' + date + '</a>');
                    $("#drinksList").append(li);
                });
                if (count == 0) {
                    var li = $('<li>');
                    li.html('Ei lisättyjä juomia');
                    $("#drinksList").append(li);
                }
            }

            function checkTimeField() {
                var success = false;
                var time = $("#time").datetimepicker("getDate");
                if (time != null && time <= new Date()) success = true;

                var button = $("#submitTime");
                button.attr("disabled", success ? "" : "disabled");
            }
        </script>
    </jsp:attribute>
    <jsp:body>
        <div class="header">
            <a class="headerButtonA" title="Kirjaudu ulos" href="logout" onClick="return confirm('Haluatko varmasti kirjautua ulos?');">
                <div class="headerButton headerButtonLeft" id="goBack">
                </div>
            </a>
            <a class="headerButtonA" title="Asetukset" href="#" onClick="toggleDialog($('#configureDrinkerDialog'), checkEmail); $('#drinkerName').focus();">
                <div class="headerButton headerButtonRight" id="configureButton">
                </div>
            </a>
            <div class="headerTextDiv">
                <h1><c:out value="${user.name}" /></h1>
            </div>
        </div>
        
        <!-- stupid css not able to center vertically properly -->
        <table style="height:200px; width:99%; margin-left:auto; margin-right:auto; margin-top:10px; margin-bottom:10px;">
            <tr style="width:100%;">
                <td class="userButton"></td>
            </tr>
        </table>
        
        <div class="header" style="margin-top: 2em;">
            <a class="headerButtonA" title="Lisää bileet" href="#" onClick="toggleDialog($('#addDrinkerDialog')); $('#nameInput').focus();">
                <div class="headerButton headerButtonRight" id="addDrinkerButton"></div>
            </a>
            <div class="headerTextDiv">
                <h1>Bileesi</h1>
            </div>
        </div>

        <div id="body">
            <c:forEach items="${parties}" var="party">
                <c:url var="viewPartyUrl" value="party?id=${party.id}" />
                <c:url var="leavePartyUrl" value="removeUserFromParty?partyId=${party.id}&userId=${user.id}" />

                <div class="party">
                    <a href="${viewPartyUrl}">
                        <span style="margin: 5px;">
                            <c:out value="${party.name}" />
                        </span>
                        <span id="partyStartTime">
                            Alkamisaika: <script type="text/javascript">document.write(formatDate(new Date(<c:out value="${party.startTime.time}" />)));</script>
                        </span>
                    </a>
                    <a class="headerButtonA" title="Poistu bileistä" onClick="return confirm('Haluatko varmasti lähteä bileistä?');" href="${leavePartyUrl}">
                        <img src="/static/images/x.png" alt="sulje" style="float:right; margin-right: 5px;" />
                    </a>
                </div>
            </c:forEach>
        </div>

        <div class="header" style="margin-top: 2em;">
            <a class="headerButtonA" title="Poista juomia" href="#" onClick="toggleDialog($('#configureDrinksDialog'), configureDrinksDialogOpened, configureDrinksDialogClosed);"><div class="headerButton headerButtonRight" id="configureDrinksButton"></div></a>
            <div class="headerTextDiv">
                <h1>Historia</h1>
            </div>
        </div>
        <div class="userHistoryContainer">
            <div id="historyGraph" style="height: 300px;"></div>
        </div>
        <div id="configureDrinksDialog" class="popupDialog">
            <span style="float: right;"><a href="#" onClick="closeDialog($('#configureDrinksDialog'), configureDrinksDialogClosed);">X</a></span>

            <div style="float:left">
                <h2>Lisää juoma ajanhetkelle</h2>
                <form method="POST" action="<c:url value="addDrinkToDate" />">
                    <input type="hidden" name="userId" value="${user.id}" />
                    <input type="text" onblur="checkTimeField();" onkeyup="checkTimeField();" onchange="checkTimeField();" name="date" id="time" value="" />
                    <input type="submit" value="Lisää" id="submitTime" />
                </form>
            </div>

            <div style="float:right">
                <h2>Poista juoma</h2>
                <ul id="drinksList">
                </ul>
            </div>
        </div>

        <div id="addDrinkerDialog" class="popupDialog">
            <span style="float: right;"><a href="#" onClick="closeDialog($('#addDrinkerDialog'));">X</a></span>

            <h2>Uudet bileet</h2>

            <form method="POST" action="<c:url value="addParty" />">
                <input type="hidden" name="userId" value="${user.id}" />
                <table>
                    <tr>
                        <th>Nimi</th>
                        <td><input id="nameInput" type="text" name="name" /></td>
                    </tr>
                    <tr>
                        <th>&nbsp;</th>
                        <td><input type="submit" value="Luo bileet" /></td>
                    </tr>
                </table>
            </form>
        </div>
                
        <div id="configureDrinkerDialog" class="popupDialog">
            <span style="float: right;">
                <a href="#" onClick="closeDialog($('#configureDrinkerDialog'));">X</a>
            </span>

            <h2>Muokkaa tietojasi</h2>

            <form method="post" action="<c:url value="modifyUser" />">
                <input type="hidden" name="userId" value="${user.id}" />                
                <table>
                    <tr>
                        <th>Nimi</th>
                        <td colspan="2"><input id="drinkerName" type="text" name="name" onkeyup="checkDrinkerFields(true);" value="<c:out value='${user.name}' />" /></td>
                    </tr>
                    <tr>
                        <th>Sähköposti</th>
                        <td><input id="email" type="text" name="email" value="<c:out value='${user.email}' />" onkeyup="checkEmail($(this).val(), '<c:out value='${user.email}'/>'); checkDrinkerFields(true);" /></td>
                        <td style="width:24px;height:24px;" id="emailCorrect">&nbsp;</td>
                    </tr>
                    <tr>
                        <th>Sukupuoli</th>
                        <td colspan="2">
                            <select name="sex">
                                <option <c:if test="${user.sex == 'MALE'}">selected="selected"</c:if> value="MALE">Mies</option>
                                <option <c:if test="${user.sex == 'FEMALE'}">selected="selected"</c:if> value="FEMALE">Nainen</option>
                            </select>
                        </td>
                    </tr>
                    <tr>
                        <th>Paino</th>
                        <td colspan="2"><input value="<c:out value='${user.weight}' />" id="drinkerWeight" type="password" name="weight" onkeyup="checkDrinkerFields(true);" autocomplete="off" /></td>
                    </tr>
                    <tr>
                        <th>&nbsp;</th>
                        <td colspan="2"><input id="submitButton" type="submit" value="Tallenna" /></td>
                    </tr>
                </table>
            </form>
        </div>
    </jsp:body>
</t:master>