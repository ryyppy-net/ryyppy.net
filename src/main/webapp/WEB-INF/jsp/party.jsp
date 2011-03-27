<%@page contentType="text/html" pageEncoding="UTF-8" isELIgnored="false"%>
<%@taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN"
   "http://www.w3.org/TR/html4/loose.dtd">
<!doctype html>
<html>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
        <title><c:out value="${party.name}" /></title>
        <script type="text/javascript" src="/static/js/jquery.js"></script>
        <script type="text/javascript" src="/static/js/drinkerchecks.js"></script>
        
        <!-- hack -->
        <!--[if lte IE 8]><script language="javascript" type="text/javascript" src="/static/js/flot/excanvas.min.js"></script><![endif]-->
        <script type="text/javascript" src="/static/js/flot/jquery.flot.js"></script>
        <script type="text/javascript" src="/static/js/flot/jquery.flot.crosshair.js"></script>
        <script type="text/javascript" src="/static/js/partygraph.js"></script>
        
        <script type="text/javascript">
            $(document).ready(function() {
                var persons = [];
                <c:forEach items="${users}" var="user">
                    persons.push(['${user.name}', '/API/users/${user.id}/show-history']);
                </c:forEach>

                render('graph', persons);
            });
        </script>
    </head>
    <body>
            Tähän sit kivasti jotain settiä.
            <br />
            <c:url var="touchUrl" value="partytouch?id=${party.id}" />
            <h1> <a href="${touchUrl}">Fäncy ui.</a> </h1>
            <br />
            
            Juomassa:
            <c:forEach items="${users}" var="user">
                <li>
                    <c:out value="${user.name}" />:
                    <c:out value="${user.promilles}" /> ‰ -
                    <c:url var="viewPartyUrl" value="viewParty?id=${party.id}&kick=${user.id}" />
                    <a href="${viewPartyUrl}">potku perseelle</a>
                </li>
            </c:forEach>
                
            Lisää uusi:
            <form method="post" action="<c:url value="linkUserToParty" />">
                <input type="hidden" name="partyId" value="${party.id}" />
                <select name="userId">
                    <c:forEach items="${allUsers}" var="user">
                        <option value="${user.id}"><c:out value="${user.name}" /></option>
                    </c:forEach>
                </select>
                <input type="submit" value="Lisää bilettäjä" onClick="forceRefresh();" />
            </form>
            <form method="post" action="<c:url value="addAnonymousUser" />">
                  <input type="hidden" name="partyId" value="${party.id}" />
                <table>
                    <tr>
                        <td>Nimi</td>
                        <td><input id="drinkerName" type="text" name="name" onBlur="checkDrinkerFields();" /></td>
                    </tr>
                    <tr>
                        <td>Sukupuoli</td>
                        <td>
                            <select name="sex">
                                <option value="MALE">Mies</option>
                                <option value="FEMALE">Nainen</option>
                            </select>
                        </td>
                    </tr>
                    <tr>
                        <td>Paino</td>
                        <td><input id="drinkerWeight" type="text" name="weight" onBlur="checkDrinkerFields();" /></td>
                    </tr>
                </table>
                <input id="submitButton" type="submit" value="Lisää bilettäjä" disabled="disabled" />
            </form>
                
            <div id="graph" style="width:600px;height:300px"></div>
    </body>
</html>
