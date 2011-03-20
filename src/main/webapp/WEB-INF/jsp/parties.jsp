<%@page contentType="text/html" pageEncoding="UTF-8" isELIgnored="false"%>
<%@taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN"
   "http://www.w3.org/TR/html4/loose.dtd">

<html>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
        <title>Bileet!</title>
        <script type="text/javascript" src="/static/js/jquery.js"></script>
        <script type="text/javascript" src="/static/js/drinkerchecks.js"></script>
    </head>
    <body>
        <h1>Käynnissä olevat bileet</h1>
        <ul>
            <c:forEach items="${parties}" var="party">
                <li>
                    <c:url var="viewPartyUrl" value="viewParty?id=${party.id}" />
                    <a href="${viewPartyUrl}"><c:out value="${party.id}" /></a>
                </li>
            </c:forEach>
        </ul>
        <form method="POST" action="<c:url value="addParty" />">
            <h2>Uudet bileet</h2>
            <table>
                <tr>
                    <td>Nimi</td><td><input type="text" name="name" /></td>
                </tr>
            </table>
            <input type="submit" value="Luo bileet" />
        </form>
            
        <h1>Olemassa olevat äijät</h1>
        <ul>
            <c:forEach items="${users}" var="user">
                <li>
                    Name: <c:out value="${user.name}" />,
                    Total drinks: <c:out value="${user.totalDrinks}" />,
                    Alcohol: <c:out value="${user.promilles}" />,
                    <c:url var="drinkUrl" value="addDrink?id=${user.id}" />
                    <a href="${drinkUrl}">Drink</a>
                </li>
            </c:forEach>
        </ul>
        <form method="post" action="<c:url value="addUser" />">
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
            <input type="submit" value="Lisää bilettäjä" />
        </form>
            
    </body>
</html>
