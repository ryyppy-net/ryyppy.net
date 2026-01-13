<%@page contentType="text/html" pageEncoding="UTF-8" isELIgnored="false"%>
<%@taglib uri="jakarta.tags.core" prefix="c" %>
<%@taglib prefix="t" tagdir="/WEB-INF/tags" %>
<%@taglib uri="http://www.springframework.org/tags" prefix="spring"%>
<t:master>
    <jsp:attribute name="customHead">
        <link rel="stylesheet" type="text/css" href="/static/css/style.css" />
        <link rel="stylesheet" href="/static/css/jquery.tooltip.css" type="text/css" media="screen" />
        <link rel="stylesheet" type="text/css" href="/static/css/login.css" />
        <link rel="preconnect" href="https://fonts.googleapis.com">
        <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
        <link href="https://fonts.googleapis.com/css2?family=Roboto:wght@500&display=swap" rel="stylesheet">

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
                    <a href="/oauth2/authorization/google" class="google-signin-button">
                        <svg class="google-logo" viewBox="0 0 18 18" xmlns="http://www.w3.org/2000/svg">
                            <g fill="none" fill-rule="evenodd">
                                <path d="M17.64 9.205c0-.639-.057-1.252-.164-1.841H9v3.481h4.844a4.14 4.14 0 0 1-1.796 2.716v2.259h2.908c1.702-1.567 2.684-3.875 2.684-6.615z" fill="#4285F4"/>
                                <path d="M9 18c2.43 0 4.467-.806 5.956-2.18l-2.908-2.259c-.806.54-1.837.86-3.048.86-2.344 0-4.328-1.584-5.036-3.711H.957v2.332A8.997 8.997 0 0 0 9 18z" fill="#34A853"/>
                                <path d="M3.964 10.71A5.41 5.41 0 0 1 3.682 9c0-.593.102-1.17.282-1.71V4.958H.957A8.996 8.996 0 0 0 0 9c0 1.452.348 2.827.957 4.042l3.007-2.332z" fill="#FBBC05"/>
                                <path d="M9 3.58c1.321 0 2.508.454 3.44 1.345l2.582-2.58C13.463.891 11.426 0 9 0A8.997 8.997 0 0 0 .957 4.958L3.964 7.29C4.672 5.163 6.656 3.58 9 3.58z" fill="#EA4335"/>
                            </g>
                        </svg>
                        <span class="google-signin-text">Kirjaudu Google-tilillä</span>
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

