<%@page contentType="text/html" pageEncoding="UTF-8" isELIgnored="false"%>
<%@taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN"
   "http://www.w3.org/TR/html4/loose.dtd">

<html>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
        <title>Ryyppy.net</title>
        <link rel="stylesheet" type="text/css" href="/static/css/style.css" />
        <link rel="stylesheet" type="text/css" href="/static/css/login.css" />

        <script type="text/javascript" src="/static/js/jquery.js"></script>
        
        <script type="text/javascript">
            function manualLogin() {
                $("#manualLogin").show(300);
                $("#openId").focus();
            }
        </script>
    </head>

<body>
    <div id="apLogo"></div>

    <div id="login">
        <h2>Kirjaudu jonkin OpenID-palvelun avulla</h2>

        <table>
            <tr>
                <td>
                    <a href="authenticate?openid=https://www.google.com/accounts/o8/id">
                        <img src="/static/images/google-icon.png" style="border:none"/>
                    </a>
                </td>
                <td>
                    <a href="authenticate?openid=https://steamcommunity.com/openid/">
                        <img src="/static/images/steam-icon2.png" style="border:none"/>
                    </a>
                </td>
                <td>
                    <a href="#manualLogin" onclick="manualLogin()">
                        <img src="/static/images/openid.png" style="border:none"/>
                    </a>
                </td>
            </tr>
        </table>

        <div id="manualLogin" style="display: none;">
            <h2>Manuaalinen OpenID-kirjautuminen</h2>

            <form id="form" method="POST" action="<c:url value="authenticate" />">
                <input name="openid" type="text" maxlength="100" id="openId" value="OPENID-TUNNUS" onFocus="$('#openId').val('');" />

                <div id="apNappula">
                    <a href="#" onClick="$('#form').get(0).submit();"
                        onmouseover="$('#submitButton').attr('src', '/static/images/kirjaudu_pullo_2.png');"
                        onmouseout="$('#submitButton').attr('src', '/static/images/kirjaudu_pullo.png');">
                        <img id="submitButton" src="/static/images/kirjaudu_pullo.png" />
                    </a>
                </div>
            </form>
        </div>
    </div>
</body>
</html>
