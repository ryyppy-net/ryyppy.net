<%@page contentType="text/html" pageEncoding="UTF-8" isELIgnored="false"%>
<%@taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml" dir="ltr" lang="fi-FI">

    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
        <title><c:out value="${party.name}" /></title>
        <link rel="stylesheet" href="/static/css/style.css" type="text/css" media="screen" />
        
        <script type="text/javascript">
            // global urls
            var dataUrl = '/API/parties/${party.id}/';
        </script>
        
        <script type="text/javascript" src="/static/js/jquery.js"></script>
        
        <!-- hack -->
        <!--[if lte IE 8]><script language="javascript" type="text/javascript" src="/static/js/flot/excanvas.min.js"></script><![endif]-->
        <script type="text/javascript" src="/static/js/flot/jquery.flot.min.js"></script>
        <script type="text/javascript" src="/static/js/flot/jquery.flot.crosshair.min.js"></script>
        <script type="text/javascript" src="/static/js/flot/jquery.flot.resize.min.js"></script>
        
        <script type="text/javascript" src="/static/js/common.js"></script>
        <script type="text/javascript" src="/static/js/userbutton.js"></script>
        <script type="text/javascript" src="/static/js/partytouch.js"></script>
        <script type="text/javascript" src="/static/js/drinkerchecks.js"></script>
        <script type="text/javascript" src="/static/js/partygraph.js"></script>
        <script type="text/javascript">
            function graphDialogOpened() {
                    // TODO clean up
                var persons = [];
<c:forEach items="${party.participants}" var="participant">
                persons.push(['${user.name}', '/API/users/${participant.id}/show-history']);
</c:forEach>
                var dialog = $('#graphDialog');
                $('#groupGraph').css('width', dialog.css('width')).css('height', dialog.css('height'));
                if (plot == null) {
                    render('groupGraph', persons);
                } 
            }
        </script>
    </head>

    <body>
        <div class="header">
            <a href="/ui/user"><div class="headerButton headerButtonLeft" id="goBack">&lt;</div></a>
            <a href="#" onClick="toggleDialog($('#graphDialog'), graphDialogOpened);"><div class="headerButton headerButtonLeft" id="graphButton">g</div></a>
            <a href="#" onClick="toggleDialog($('#addDrinkerDialog'));"><div class="headerButton headerButtonRight" id="addDrinkerButton">+</div></a>
            <a href="#" onClick="toggleDialog($('#kickDrinkerDialog'));"><div class="headerButton headerButtonRight" id="kickDrinkerButton">k</div></a>
            <div class="headerTextDiv">
                <h1 id="topic"><a href="viewParty?id=<c:out value="${party.id}" />"><c:out value="${party.name}" /></a></h1>
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
                            <input type="text" name="email" onkeyup="getIdByEmail($(this).val(), '<c:out value="${party.id}" />');" onblur="getIdByEmail($(this).val(), '<c:out value="${party.id}" />');"/>
                        </td>
                        <td style="display:none;width:24px;height:24px;" id="emailCorrect"></td>
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
                <c:forEach var="participant" items="${party.participants}">
                    <c:url var="leavePartyUrl" value="removeUserFromParty?partyId=${party.id}&userId=${participant.id}" />
                    <c:if test="${participant.id != user.id}">
                        <li><a href="${leavePartyUrl}"><c:out value="${participant.name}" /> </a></li>
                    </c:if>
                </c:forEach>
            </ul>
        </div>
                
    </body>
</html>

