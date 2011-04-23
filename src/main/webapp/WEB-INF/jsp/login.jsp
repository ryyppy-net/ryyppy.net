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
        </script>
        <script type="text/javascript" src="/static/js/login.js"></script>
        <meta name="viewport" content="initial-scale=1.0, maximum-scale=1.0, user-scalable=0;">
    </jsp:attribute>
    
    <jsp:body>
        <div id="logoContainer">
            <img id="logo" src="/static/images/logo_ryyppy.png" alt="Ryyppy.net" title="Ryyppy.net" />
        </div>
        
        <div class="login">
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
        <div class="login">

            <p style="margin-top: 20px;">
                <iframe src="http://www.facebook.com/plugins/like.php?href=http%3A%2F%2Fwww.facebook.com%2Fpages%2FRyyppynet%2F151477181585164&amp;layout=button_count&amp;show_faces=false&amp;width=90&amp;action=like&amp;font&amp;colorscheme=dark&amp;height=21" scrolling="no" frameborder="0" style="border:none; overflow:hidden; width:90px; height:21px;" allowTransparency="true"></iframe>
            </p>
            
            <p>
                <iframe id="youtube" title="YouTube video player" src="http://www.youtube.com/embed/A6JWd3-yzCM" frameborder="0" allowfullscreen></iframe>
            </p>

            <p>
                Yhteensä juomia juotu: <c:out value="${totalDrinkCount}" />
            </p>
        </div>
    </jsp:body>
</t:master>

