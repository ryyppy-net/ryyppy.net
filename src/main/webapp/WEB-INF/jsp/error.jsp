<%@page contentType="text/html" pageEncoding="UTF-8" isELIgnored="false"%>
<%@taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@taglib prefix="t" tagdir="/WEB-INF/tags" %>
<t:master title="Virhe">
    <jsp:attribute name="customHead">
        <link rel="stylesheet" type="text/css" href="/static/css/login.css" />
        <script type="text/javascript" src="/static/js/jquery.js"></script>
    </jsp:attribute>
    <jsp:body>
        <div id="apLogo"></div>
        <div id="login">
            <h2>Virhe tapahtui!</h2>
            <p>Mahdollisia syitä:</p>
            <ul>
                <li>sinulla ei riitä oikeudet yrittämällesi sivulle</li>
                <li>yritit mennä sivulle, jota ei ole olemassa</li>
                <li>järjestelmässä on bugi, <a href="mailto:info@ryyppy.net">kerro siitä meille</a></li>
                <li>töhötit jotain muuta</li>
            </ul>
            <p>Jatka <a href="/ui/checklogin">etusivulle</a> ja kokeile uudelleen.</p>
        </div>
    </jsp:body>
</t:master>