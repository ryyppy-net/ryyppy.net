<%@page contentType="text/html" pageEncoding="UTF-8" isELIgnored="false"%>
<%@taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN"
   "http://www.w3.org/TR/html4/loose.dtd">
<!doctype html>
<html>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
        <title>Uusi juoja</title>
        <script type="text/javascript" src="/static/js/jquery.js"></script>
        <script type="text/javascript" src="/static/js/drinkerchecks.js"></script>
    </head>
    <body>
        Olet näköjään ensimmäistä kertaa, rekisteröidypäs. <br>
        
        <form method="post" action="<c:url value="addUser" />">
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
    </body>
</html>
