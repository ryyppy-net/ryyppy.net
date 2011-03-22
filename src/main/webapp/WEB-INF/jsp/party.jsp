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
                        <td><input id="drinkerName" type="text" name="name" onBlur="checkDrinkerName();" /></td>
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
                        <td><input type="text" name="weight" /></td>
                    </tr>
                </table>
                <input type="submit" value="Lisää bilettäjä" onClick="forceRefresh();" />
            </form>
        
    </body>
</html>
