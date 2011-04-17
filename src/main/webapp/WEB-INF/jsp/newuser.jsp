<%@page contentType="text/html" pageEncoding="UTF-8" isELIgnored="false"%>
<%@taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<!DOCTYPE html>

<html>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
        <link rel="stylesheet" href="/static/css/style.css" type="text/css" media="screen" />
        <title>Uusi juoja</title>
        <script type="text/javascript" src="/static/js/jquery.js"></script>
        <script type="text/javascript" src="/static/js/common.js"></script>
        <script type="text/javascript" src="/static/js/drinkerchecks.js"></script>
        <script type="text/javascript">
            $(document).ready(function() {
                openDialog($("#addDrinkerDialog"));
                $("#drinkerName").focus();
            });
        </script>
        <script type="text/javascript">
          var _gaq = _gaq || [];
          _gaq.push(['_setAccount', 'UA-22483744-1']);
          _gaq.push(['_trackPageview']);

          (function() {
            var ga = document.createElement('script'); ga.type = 'text/javascript'; ga.async = true;
            ga.src = ('https:' == document.location.protocol ? 'https://ssl' : 'http://www') + '.google-analytics.com/ga.js';
            var s = document.getElementsByTagName('script')[0]; s.parentNode.insertBefore(ga, s);
          })();
        </script>
    </head>
    <body>
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
                        <td><input id="drinkerName" type="text" name="name" onchange="checkDrinkerFields(true);" /></td>
                        <td>&nbsp;</td>
                    </tr>
                    <tr>
                        <th>Sähköposti</th>
                        <td><input id="email" type="text" name="email" onchange="checkEmail($(this).val()); checkDrinkerFields(true);" /></td>
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
                        <td><input id="drinkerWeight" type="password" name="weight" onchange="checkDrinkerFields(true);" /></td>
                        <td>&nbsp;</td>
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
