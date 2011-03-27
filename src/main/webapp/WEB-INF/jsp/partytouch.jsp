<%@page contentType="text/html" pageEncoding="UTF-8" isELIgnored="false"%>
<%@taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml" dir="ltr" lang="fi-FI">

    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
        <title>Alkoholilaskuri</title>
        <link rel="stylesheet" href="/static/css/style.css" type="text/css" media="screen" />
        <script type="text/javascript">
            // shitty hack, plz fix
            var addDrinkUrl = '/API/users/_userid_/add-drink';
            var historyUrl = '/API/users/_userid_/show-history';
            var dataUrl = '/API/parties/${party.id}/';
        </script>
        <script type="text/javascript" src="/static/js/jquery.js"></script>
        <script type="text/javascript" src="/static/js/flot/jquery.flot.js"></script>
        <script type="text/javascript" src="/static/js/flot/jquery.flot.resize.js"></script>
        <script type="text/javascript" src="/static/js/graph.js"></script>
        <script type="text/javascript" src="/static/js/partytouch.js"></script>
        <script type="text/javascript" src="/static/js/drinkerchecks.js"></script>
    </head>

    <body>
        <div id="header">
            <h1 id="topic"><c:out value="${party.id}" /></h1>
            <a href="http://localhost:8080/ui/viewParty?id=${party.id}"><div id="goBack">&lt;</div></a>
            <a href="#" onClick="openAddDrinkerDialog();"><div id="addDrinkerButton">+</div></a>
        </div>

        <div id="body">
            <table id="drinkers">
            </table>
        </div>

        <div id="addDrinkerDialog" style="display: none; position: absolute;">
            <span style="float: right;"><a href="#" onClick="closeAddDrinkerDialog();">X</a></span>

            <h2>Lisää rekisteröitynyt käyttäjä</h2>
            <form method="post" action="<c:url value="linkUserToParty" />">
                <input type="hidden" name="partyId" value="${party.id}" />
                <table>
                    <tr>
                        <th>Nimi</th>
                        <td>
                        <select name="userId">
                            <c:forEach items="${allUsers}" var="user">
                                <option value="${user.id}"><c:out value="${user.name}" /></option>
                            </c:forEach>
                        </select>
                    </td></tr>
                    <tr>
                        <th>&nbsp;</th>
                        <td><input type="submit" value="Lisää bilettäjä" onClick="forceRefresh();" /></td>
                    </tr>
                </table>
            </form>

            <h2>Lisää vieras</h2>
            <form method="post" action="<c:url value="addAnonymousUser" />">
                  <input type="hidden" name="partyId" value="${party.id}" />
                <table>
                    <tr>
                        <th>Nimi</th>
                        <td><input id="drinkerName" type="text" name="name" onBlur="checkDrinkerFields();" /></td>
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
                        <td><input id="drinkerWeight" type="text" name="weight" onBlur="checkDrinkerFields();" /></td>
                    </tr>
                    <tr>
                        <th>&nbsp;</th>
                        <td><input id="submitButton" type="submit" value="Lisää bilettäjä" onClick="forceRefresh();" disabled="disabled" /></td>
                    </tr>
                </table>
            </form>
        </div>
    </body>
</html>

