<%@page contentType="text/html" pageEncoding="UTF-8" isELIgnored="false"%>
<%@taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@taglib prefix="t" tagdir="/WEB-INF/tags" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<t:master title="${party.name}">
    <jsp:attribute name="customHead">
        <link rel="stylesheet" href="/static/css/jquery.tooltip.css" type="text/css" media="screen" />
        
        <script type="text/javascript">
            // global urls
            var dataUrl = '/API/parties/${party.id}/';
            var updateInterval = 2 * 60 * 1000;
        </script>
        <script type="text/javascript" src="/static/js/jquery.tooltip.min.js"></script>
        
        <!-- hack -->
        <!--[if lte IE 8]><script language="javascript" type="text/javascript" src="/static/js/flot/excanvas.min.js"></script><![endif]-->
        <script type="text/javascript" src="/static/js/flot/jquery.flot.min.js"></script>
        <script type="text/javascript" src="/static/js/flot/jquery.flot.crosshair.min.js"></script>
        <script type="text/javascript" src="/static/js/flot/jquery.flot.resize.min.js"></script>
        
        <script type="text/javascript" src="/static/js/common.js"></script>
        <script type="text/javascript" src="/static/js/userbutton.js"></script>
        <script type="text/javascript" src="/static/js/drinkerchecks.js"></script>
        <script type="text/javascript" src="/static/js/partygraph.js"></script>
        <script type="text/javascript">
            var graph = null;
            var graphInterval = null;
            var graphVisible = false;
            
            function updateGroupGraph() {
                if (graph != null && graphVisible)
                    graph.update();
            }
            
            function graphDialogOpened() {
                graphVisible = true;
                var element = $('#groupGraph');
                var dialog = $('#graphDialog');
                element.css('width', dialog.css('width')).css('height', (parseInt(dialog.css('height'), 0) - 10) + 'px');
                
                if (graph == null) {
                    var users = [];
<c:forEach items="${party.participants}" var="participant">
                    users.push({name: '${participant.name}', id:'${participant.id}'});
</c:forEach>
                    graph = new GroupGraph(users, element);
                }
                graph.options.legend = { position:'nw' };
                updateGroupGraph();
                if (!graphInterval)
                    graphInterval = setInterval(updateGroupGraph, updateInterval);
            }

            function graphDialogClosed() {
                graphVisible = false;
                if (graphInterval)
                    graphInterval = clearInterval(graphInterval);
            }
        </script>
        <script type="text/javascript" src="/static/js/partytouch.js"></script>
    </jsp:attribute>
    <jsp:body>
        <div class="header">
            <a class="headerButtonA" title="Takaisin" href="/ui/user"><div class="headerButton headerButtonLeft" id="goBack"></div></a>
            <a class="headerButtonA" title="Näytä graafi" href="#" onClick="toggleDialog($('#graphDialog'), graphDialogOpened, graphDialogClosed);"><div class="headerButton headerButtonLeft" id="graphButton"></div></a>
            <a class="headerButtonA" title="Lisää bilettäjä" href="#" onClick="toggleDialog($('#addDrinkerDialog')); $('#emailInput').focus();"><div class="headerButton headerButtonRight" id="addDrinkerButton"></div></a>
            <a class="headerButtonA" title="Poista bilettäjä" href="#" onClick="toggleDialog($('#kickDrinkerDialog'));"><div class="headerButton headerButtonRight" id="kickDrinkerButton"></div></a>
            <div class="headerTextDiv">
                <h1 id="topic"><c:out value="${party.name}" /></h1>
            </div>
        </div>

        <div id="body">
            <table id="drinkers">
            </table>
        </div>
            
        <div id="graphDialog">
            <span style="float: right;"><a href="#" onClick="closeDialog($('#graphDialog'));">X</a></span>
            <div style="margin-top: 13px;" id="groupGraph">
            </div>
        </div>

        <div id="addDrinkerDialog" class="popupDialog">
            <span style="float: right;"><a href="#" onClick="closeDialog($('#addDrinkerDialog'));">X</a></span>

            <h2>Lisää rekisteröitynyt käyttäjä</h2>
            <form method="post" action="<c:url value="linkUserToParty" />">
                <input type="hidden" name="partyId" value="${party.id}" />
                <input type="hidden" id="userId" name="userId" />
                
                <table>
                    <tr>
                        <th>Sähköpostiosoite</th>
                        <td>
                            <input id="emailInput" type="text" name="email" onkeyup="getIdByEmail($(this).val(), '<c:out value="${party.id}" />');" onblur="getIdByEmail($(this).val(), '<c:out value="${party.id}" />');"/>
                        </td>
                        <td id="emailCorrect"></td>
                    </tr>
                    <tr>
                        <th>&nbsp;</th>
                        <td colspan="2"><input id="linkUserButton" type="submit" value="Lisää bilettäjä" disabled="disabled" onClick="forceRefresh();" /></td>
                    </tr>
                </table>
            </form>

            <h2>Lisää vieras</h2>
            <form method="post" action="<c:url value="addAnonymousUser" />">
                <input type="hidden" name="partyId" value="${party.id}" />
                <table>
                    <tr>
                        <th>Nimi</th>
                        <td><input id="drinkerName" type="text" name="name" onkeyup="checkDrinkerFields(false);" /></td>
                    </tr>
                    <tr>
                        <th>Sukupuoli</th>
                        <td>
                            <select name="sex">
                                <option value="MALE">Mies</option>
                                <option value="FEMALE">Nainen</option>
                            </select>
                        </td>
                    </tr>
                    <tr>
                        <th>Paino</th>
                        <td><input id="drinkerWeight" type="password" name="weight" onkeyup="checkDrinkerFields(false);" /></td>
                    </tr>
                    <tr>
                        <th>&nbsp;</th>
                        <td><input id="submitButton" type="submit" value="Lisää bilettäjä" onClick="forceRefresh();" disabled="disabled" /></td>
                    </tr>
                </table>
            </form>
        </div>
                
        <div style="width:300px;" id="kickDrinkerDialog" class="popupDialog">
            <span style="float: right;"><a href="#" onClick="closeDialog($('#kickDrinkerDialog'));">X</a></span>

            <h2>Poista käyttäjä bileistä</h2>
            <ul>
                <c:choose>
                    <c:when test="${fn:length(party.participants) < 2}">
                        <li>Ei potkittavia juojia</li>
                    </c:when>
                    <c:otherwise>
                        <c:forEach var="participant" items="${party.participants}">
                            <c:url var="leavePartyUrl" value="removeUserFromParty?partyId=${party.id}&userId=${participant.id}" />
                            <c:if test="${participant.id != user.id}">
                                <li><a href="#" onClick="if (confirm('Poistetaanko ${participant.name} bileistä?')) $.get('${leavePartyUrl}', function() {location.reload(true);});"><c:out value="${participant.name}" /> </a></li>
                            </c:if>
                        </c:forEach>
                    </c:otherwise>
                </c:choose>
            </ul>
        </div>
    </jsp:body>
</t:master>

