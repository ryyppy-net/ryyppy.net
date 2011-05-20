<%@page contentType="text/html" pageEncoding="UTF-8" isELIgnored="false"%>
<%@taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@taglib prefix="t" tagdir="/WEB-INF/tags" %>
<%@taglib uri="http://www.springframework.org/tags" prefix="spring"%>
<t:master title="Ryyppy.net - Virhe sisäänkirjautumisessa!">
    <jsp:attribute name="customHead">
        <link rel="stylesheet" type="text/css" href="/static/css/login.css" />
        <script type="text/javascript" src="/static/js/login.js"></script>
    </jsp:attribute>
    <jsp:body>
        <div id="logoContainer">
            <img id="logo" src="/static/images/logo_ryyppy.png" alt="Ryyppy.net" title="Ryyppy.net" />
        </div>
    
        <div class="login" style="text-align:left;padding-left:30px;">
            <h2><spring:message code="loginerror.title" /></h2>
            <p><spring:message code="loginerror.reasons" /></p>
            <ul>
                <li><spring:message code="loginerror.list.bad_openid" /></li>
                <li><spring:message code="loginerror.list.no_2_0_standard" /></li>
                <li><spring:message code="loginerror.list.login_failed" /></li>
                <li><spring:message code="loginerror.list.bug" />
                    <a href="mailto:ryyppy.net@gmail.com"><spring:message code="loginerror.list.tell_us" /></a></li>
                <li><spring:message code="loginerror.list.you_fucked_up" /></li>
            </ul> 
            <p>
                <spring:message code="loginerror.continue" />
                <a href="/"><spring:message code="loginerror.to_frontpage" /></a>
                <spring:message code="loginerror.try_again" />
            </p>
        </div>
    </jsp:body>
</t:master>
