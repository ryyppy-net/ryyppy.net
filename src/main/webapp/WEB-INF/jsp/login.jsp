<%@page contentType="text/html" pageEncoding="UTF-8" isELIgnored="false"%>
<%@taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@taglib prefix="t" tagdir="/WEB-INF/tags" %>
<%@taglib uri="http://www.springframework.org/tags" prefix="spring"%>
<t:master>
    <jsp:attribute name="customHead">
        <link rel="stylesheet" type="text/css" href="/static/css/style.css" />
        <link rel="stylesheet" href="/static/css/jquery.tooltip.css" type="text/css" media="screen" />
        <link rel="stylesheet" type="text/css" href="/static/css/login.css" />
        
        <script type="text/javascript">
            function manualLogin() {
                $("#manualLogin").show(300);
                $("#openId").focus();
            }
            
            function login(openId){
                $('#openId').val(openId);
                $('#form').submit();
            }
        </script>
        <script type="text/javascript" src="/static/js/login.js"></script>
        <script type="text/javascript" src="https://apis.google.com/js/plusone.js"></script>
    </jsp:attribute>
    
    <jsp:body>
        <div id="logoContainer">
            <img id="logo" src="/static/images/logo_ryyppy.png" alt="Ryyppy.net" title="Ryyppy.net" />
            
            <p class="totalDrinkCount">
                <spring:message code="login.already_n_drinks" arguments="${totalDrinkCount}" />
            </p>
            
            <p class="langSelection">
                <a href="?lang=en">In English</a> | <a href="?lang=fi">Suomeksi</a>
            </p>
        </div>
        
        <div class="login">
            <h2> <spring:message code="login.login_title" /> </h2>
            
            <table>
                <tr>
                    <td>
                        <img class="loginLogo" onclick="login('https://www.google.com/accounts/o8/id')" src="/static/images/google-icon.png" alt="Google" />
                    </td>
                    <td>
                        <img class="loginLogo" onclick="login('https://steamcommunity.com/openid/')" src="/static/images/steam-icon2.png" alt="Steam" />
                    </td>
                    <td>
                        <a href="facebook"><img class="loginLogo" src="/static/images/f_logo.png" alt="Facebook" /></a>
                    </td>
                    <td>
                        <a href="#manualLogin" onclick="manualLogin()">
                            <img class="loginLogo" src="/static/images/openid.png" alt="OpenID" />
                        </a>
                    </td>
                </tr>
            </table>
            
            <p><spring:message code="login.security_info" /></p>
            <p><spring:message code="login.more_security_info" /></p>

            <div id="manualLogin" style="display: none;">
                <h2><spring:message code="login.login_manual"/></h2>
                <form id="form" method="POST" action="<c:url value="/ui/openId?is_return=true" />">
                    <input name="openid_identifier" type="text" maxlength="100" id="openId" />
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
            <h2><spring:message code="login.info.title" /></h2>
            
            <p>
                <spring:message code="login.info.content" />
            </p>
            
            <p>
                <spring:message code="login.info.watch" />
                <a href="http://www.youtube.com/embed/A6JWd3-yzCM"><spring:message code="login.info.video" /></a>
                <spring:message code="login.info.youtube" />
            </p>
        </div>
                    
        <div style="text-align: center; margin-top: 20px;">
            <p>
                <iframe src="http://www.facebook.com/plugins/like.php?href=http%3A%2F%2Fwww.facebook.com%2Fpages%2FRyyppynet%2F151477181585164&amp;layout=button_count&amp;show_faces=false&amp;width=90&amp;action=like&amp;font&amp;colorscheme=dark&amp;height=21" scrolling="no" frameborder="0" style="border:none; overflow:hidden; width:90px; height:21px; margin-right:20px;" allowTransparency="true"></iframe>
                <g:plusone size="medium" href="http://ryyppy.net"></g:plusone>
            </p>
        </div>
    </jsp:body>
</t:master>

