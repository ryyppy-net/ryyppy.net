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
                
                repaint();
            });
            
            $(window).resize(function() {
                repaint();
            });
            
            function repaint() {
                var windowWidth = $(window).width();
                var bestWidth = Math.min(600, windowWidth - 20);
                $("#registration").width(bestWidth);
            }
        </script>
        
        <meta name="viewport" content="initial-scale=1.0, maximum-scale=1.0, user-scalable=0;">
    </jsp:attribute>
    <jsp:body>
        <div id="header">
            <h1>Tervetuloa!</h1>
        </div>

        <div id="container">
            <div id="registration" class="dialog marginAuto">
                <h2>Rekisteröinti</h2>

                <p>Olet näemmä ensimmäistä kertaa tällä sivulla. Ole hyvä ja täytä allaolevat tiedot, niin aletaan ryyppäämään!</p>

                <form method="post" action="addUser">
                    <label for="drinkerName">Nimi</label><br />
                    <input id="drinkerName" type="text" name="name" onkeyup="checkDrinkerFields(true);" /><br />

                    <label for="email">Sähköposti</label><br />
                    <input id="email" type="text" name="email" onkeyup="checkEmail($(this).val()); checkDrinkerFields(true);" />
                    <span id="emailCorrect">&nbsp;</span><br />

                    <label for="sex">Sukupuoli</label><br />
                    <select name="sex">
                        <option value="MALE">Mies</option>
                        <option value="FEMALE">Nainen</option>
                    </select><br />

                    <label for="drinkerWeight">Paino</label><br />
                    <input id="drinkerWeight" type="password" name="weight" onkeyup="checkDrinkerFields(true);" /><br />

                    <input id="submitButton" type="submit" value="Lisää bilettäjä" />
                </form>
            </div>
        </div>
    </jsp:body>
</t:master>