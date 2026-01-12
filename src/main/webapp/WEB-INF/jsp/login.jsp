<%@page contentType="text/html" pageEncoding="UTF-8" isELIgnored="false"%>
<%@taglib uri="jakarta.tags.core" prefix="c" %>
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
    </jsp:attribute>
    
    <jsp:body>
        <div id="logoContainer">
            <img id="logo" src="/static/images/logo_ryyppy.png" alt="Ryyppy.net" title="Ryyppy.net" />
            
            <p class="totalDrinkCount">
                <spring:message code="login.already_n_drinks" arguments="${totalDrinkCount}" />
            </p>
        </div>
        
        <div class="login">
            <h2> <spring:message code="login.login_title" /> </h2>
            
            <p>
                <form action="/login" method="post" >
                    <label for="username">Käyttäjätunnus</label>
                    <input id="username" type="text" name="username"><br>
                    <label for="password">Salasana</label>
                    <input id="password" type="password" name="password"><br>
                    <input type="submit" value="Kirjaudu">
                </form>
            </p>

            <c:if test="${googleAuthEnabled}">
                <p style="text-align: center; margin: 20px 0;">
                    <a href="/oauth2/authorization/google" style="display: inline-block; padding: 10px 20px; background-color: #4285f4; color: white; text-decoration: none; border-radius: 4px; font-weight: 500;">
                        Kirjaudu Google-tilillä
                    </a>
                </p>
            </c:if>

            <p>Eikö sinulla ole vielä tunnuksia? <a href="newuser">Rekisteröi itsesi saadaksesi tunnukset</a>
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

