<%@page contentType="text/html" pageEncoding="UTF-8" isELIgnored="false"%>
<%@taglib uri="jakarta.tags.core" prefix="c" %>
<%@taglib prefix="t" tagdir="/WEB-INF/tags" %>
<%@taglib uri="http://www.springframework.org/tags" prefix="spring"%>
<t:master>
    <jsp:attribute name="customHead">
        <link rel="stylesheet" type="text/css" href="<c:url value="/static/css/login.css"/>" />
        <script type="text/javascript" src="<c:url value="/static/js/login.js"/>"></script>
        <script type="text/javascript">
            $(document).ready(function() {
                $('#d_clip_button').click(function() {
                    navigator.clipboard.writeText('<c:out value="${passphrase}" />').then(function() {
                        alert('<spring:message code="passphrase.copied" />');
                    });
                });
            });
        </script>
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
            <br />
            <spring:message code="passphrase.copy_clipboard" />
            <div id="d_clip_container" style="position:relative">
                <button type="button" id="d_clip_button"><spring:message code="passphrase.copy_clipboard_button" /></button>
            </div>
        </div>
    </jsp:body>
</t:master>

