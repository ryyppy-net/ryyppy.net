<%@page contentType="text/html" pageEncoding="UTF-8" isELIgnored="false"%>
<%@taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@taglib prefix="t" tagdir="/WEB-INF/tags" %>

<t:master title="Uusi juoja">
    <jsp:attribute name="customHead">
        <script type="text/javascript" src="/static/js/common.js"></script>
        <script type="text/javascript" src="/static/js/drinkerchecks.js"></script>
        <script type="text/javascript">
            $(document).ready(function() {
                openDialog($("#addDrinkerDialog"));
                $("#drinkerName").focus();
            });
        </script>
    </jsp:attribute>
    <jsp:body>
        <div id="header">
            <h1>Tervetuloa!</h1>
        </div>

        <div id="addDrinkerDialog" class="popupDialog">
            <h2>Rekisteröinti</h2>

            <p>Olet näemmä ensimmäistä kertaa tällä sivulla. Ole hyvä ja täytä allaolevat tiedot, niin aletaan ryyppäämään!</p>

            <form method="post" action="addUser">
                <table>
                    <tr>
                        <th>Nimi</th>
                        <td><input id="drinkerName" type="text" name="name" onkeyup="checkDrinkerFields(true);" /></td>
                        <td>&nbsp;</td>
                    </tr>
                    <tr>
                        <th>Sähköposti</th>
                        <td><input id="email" type="text" name="email" onkeyup="checkEmail($(this).val()); checkDrinkerFields(true);" /></td>
                        <td id="emailCorrect">&nbsp;</td>
                    </tr>
                    <tr>
                        <th>Sukupuoli</th>
                        <td>
                            <select name="sex">
                                <option value="MALE">Mies</option>
                                <option value="FEMALE">Nainen</option>
                            </select>
                        </td>
                        <td>&nbsp;</td>
                    </tr>
                    <tr>
                        <th>Paino</th>
                        <td><input id="drinkerWeight" type="password" name="weight" onkeyup="checkDrinkerFields(true);" /></td>
                        <td>&nbsp;</td>
                    </tr>
                    <tr>
                        <th>&nbsp;</th>
                        <td><input id="submitButton" type="submit" value="Lisää bilettäjä" /></td>
                    </tr>
                </table>
            </form>
        </div>
    </jsp:body>
</t:master>