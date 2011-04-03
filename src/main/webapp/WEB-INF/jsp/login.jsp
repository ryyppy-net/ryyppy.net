<%@page contentType="text/html" pageEncoding="UTF-8" isELIgnored="false"%>
<%@taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN"
   "http://www.w3.org/TR/html4/loose.dtd">

<html>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
        <title>Känniin</title>
        <link rel="stylesheet" type="text/css" href="/static/css/login.css" />

        <script type="text/javascript" src="/static/js/jquery.js"></script>
        <script type="text/javascript" src="/static/js/InFieldLabels/src/jquery.infieldlabel.min.js"></script>
        <script type="text/javascript">
          $(document).ready(function() {
            $("label").inFieldLabels({fadeOpacity: 0.5, fadeDuration: 300});
            $("#openId").focus();
          });
        </script>
    </head>

<body>
	<div id="apLogo"></div>

	<div id="login">
	
		<div id="loginText">
                    <a href="http://en.wikipedia.org/wiki/Openid#OpenID_Providers">
			<img src="/static/images/kirjaudu.png" />
                    </a>
		</div>
	
                <form id="form" method="POST" action="<c:url value="authenticate" />">
                    <div id="tekstilaatikko">
                                <label id="openIdLabel" for="openId">OPENID-TUNNUS</label>
                                <input name="openid" type="text" maxlength="100" id="openId"> </input>
                    </div>
                    <div id="apNappula">
                            <a href="#" onClick="$('#form').get(0).submit();" 
                                onmouseover="$('#submitButton').attr('src', '/static/images/kirjaudu_pullo_2.png');" 
                                onmouseout="$('#submitButton').attr('src', '/static/images/kirjaudu_pullo.png');">
                                <img id="submitButton" src="/static/images/kirjaudu_pullo.png" style="border:none"/>
                            </a>
                    </div>
                </form>

                <div id="altLogin">
			<div id="apGoogleIcon">
				<a href="authenticate?openid=https://www.google.com/accounts/o8/id">
                                    <img src="/static/images/google-icon.png" style="border:none"/>
				</a>
			</div>
			<div id="apSteam">
				 <a href="authenticate?openid=https://steamcommunity.com/openid/">
                                    <img src="/static/images/steam-icon2.png" style="border:none"/>
				 </a>
			</div>
		</div>
	</div>

</body>
</html>
