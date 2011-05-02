<%@page contentType="text/html" pageEncoding="UTF-8" isELIgnored="false"%>
<%@taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@taglib prefix="t" tagdir="/WEB-INF/tags" %>
<%@taglib uri="http://www.springframework.org/tags" prefix="spring"%>
<t:master title="Ryyppy.net - Virhe!">
    <jsp:attribute name="customHead">
        <link rel="stylesheet" type="text/css" href="/static/css/login.css" />
        <script type="text/javascript" src="/static/js/login.js"></script>
    </jsp:attribute>
    <jsp:body>
        <div id="logoContainer">
            <img id="logo" src="/static/images/logo_ryyppy.png" alt="Ryyppy.net" title="Ryyppy.net" />
        </div>
        
        <div class="login" style="text-align:left;">
            <h2> <spring:message code="error.title" /></h2>
            <p><spring:message code="error.reasons" /></p>
            <ul>
                <li><spring:message code="error.list.unauthorized" /></li>
                <li><spring:message code="error.list.page_doesnt_exist" /></li>
                <li><spring:message code="error.list.bug" /> <a href="mailto:ryyppy.net@gmail.com"><spring:message code="error.list.tell_us" /></a></li>
                <li><spring:message code="error.list.you_fucked_up" /></li>
            </ul>
            <p><spring:message code="error.continue" />
                <a href="/"><spring:message code="error.to_frontpage" /></a> 
                <spring:message code="error.try_again" />
            </p>
        </div>
    </jsp:body>
</t:master>