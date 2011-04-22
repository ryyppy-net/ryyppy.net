<%@page contentType="text/html" pageEncoding="UTF-8" isELIgnored="false"%>
<%@taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@taglib prefix="t" tagdir="/WEB-INF/tags" %>
<t:master>
    <jsp:attribute name="customHead">
        <link rel="stylesheet" type="text/css" href="/static/css/login.css" />
        <script type="text/javascript">
            function manualLogin() {
                $("#manualLogin").show(300);
                $("#openId").focus();
            }
            
            $(document).ready(function() {
                repaint();
            });
            
            $(window).resize(function() {
                repaint();
            });
            
            function repaint() {
                var windowWidth = $(window).width();
                var bestWidth = Math.min(600, windowWidth - 20);
                $("#logoContainer").width(bestWidth);
                $("#login").width(bestWidth);
            }
        </script>
        <meta name="viewport" content="initial-scale=1.0, maximum-scale=1.0, user-scalable=0;">
    </jsp:attribute>
    
    <jsp:body>
        <div id="logoContainer">
            <img id="logo" src="/static/images/logo_ryyppy.png" alt="Ryyppy.net" title="Ryyppy.net" />
        </div>
        
        <div id="login">
            <h2>Kirjaudu jonkin OpenID-palvelun avulla</h2>

            <table>
                <tr>
                    <td>
                        <a href="authenticate?openid=https://www.google.com/accounts/o8/id">
                            <img src="/static/images/google-icon.png" style="border:none" alt="Google" />
                        </a>
                    </td>
                    <td>
                        <a href="authenticate?openid=https://steamcommunity.com/openid/">
                            <img src="/static/images/steam-icon2.png" style="border:none" alt="Steam" />
                        </a>
                    </td>
                    <td>
                        <a href="#manualLogin" onclick="manualLogin()">
                            <img src="/static/images/openid.png" style="border:none" alt="OpenID" />
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
                            <img id="submitButton" src="/static/images/kirjaudu_pullo.png" alt="kirjaudu" />
                        </a>
                    </div>
                </form>
            </div>
        </div>
    </jsp:body>
</t:master>

