<%@page contentType="text/html" pageEncoding="UTF-8" isELIgnored="false"%>
<%@taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN"
   "http://www.w3.org/TR/html4/loose.dtd">

<html>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
        <title>Känniin</title>
    </head>
    <body>
        <form method="POST" action="<c:url value="authenticate" />">
            <h2>OpenId autentikaatio</h2>
            <table>
                <tr>
                    <td>OpenId-tunnus</td><td><input type="text" name="openid" /></td>
                </tr>
            </table>
            <input type="submit" value="Tunnistaudu" />
        </form>
            
        Tai kirjaudu käyttämällä <a href="authenticate?openid=https://www.google.com/accounts/o8/id">Google-tiliäsi</a>
    </body>
</html>
