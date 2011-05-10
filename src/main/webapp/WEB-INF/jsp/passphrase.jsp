<%@page contentType="text/html" pageEncoding="UTF-8" isELIgnored="false"%>
<%@taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@taglib prefix="t" tagdir="/WEB-INF/tags" %>
<%@taglib uri="http://www.springframework.org/tags" prefix="spring"%>
<t:master>
    <jsp:attribute name="customHead">
        <link rel="stylesheet" type="text/css" href="/static/css/login.css" />
        <script type="text/javascript" src="/static/js/login.js"></script>
    </jsp:attribute>
    
    <jsp:body>
        <div class="login">
            <h2><spring:message code="passphrase.header" /></h2>
            
            <p>
                <spring:message code="passphrase.info" />
            </p>
            
            <form>
                <input type="text" readonly="readonly" size="35" name="passphrase" value="<c:out value="${passphrase}" />" /> <br />
                <input type="button" value="<spring:message code="passphrase.generate_new" />" onclick="location.href='passphrase-generate'" />
            </form>
        </div>
    </jsp:body>
</t:master>

