<%@page contentType="text/html" pageEncoding="UTF-8" isELIgnored="false"%>
<%@taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN"
   "http://www.w3.org/TR/html4/loose.dtd">
<!doctype html>
<html>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
        <link rel="stylesheet" href="/static/css/style.css" type="text/css" media="screen" />
        <title>Uusi juoja</title>
        <script type="text/javascript" src="/static/js/jquery.js"></script>
        <script type="text/javascript" src="/static/js/common.js"></script>
        <script type="text/javascript" src="/static/js/drinkerchecks.js"></script>
        <script type="text/javascript">
            $(document).ready(function() {
                openPopupDialog();
            });
        </script>
    </head>
    <body>
        <div id="header">
            <h1>Tervetuloa!</h2>
        </div>

        <div id="addDrinkerDialog" style="width: 400px;">
            <h2>Rekisteröinti</h2>

            <p>Olet näemmä ensimmäistä kertaa tällä sivulla. Ole hyvä ja täytä allaolevat tiedot, niin aletaan ryyppäämään!</p>

            <form method="post" action="<c:url value="addUser" />">
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
                        <td><input id="submitButton" type="submit" value="Lisää bilettäjä" disabled="disabled" /></td>
                    </tr>
                </table>
            </form>
        </div>
    </body>
</html>
