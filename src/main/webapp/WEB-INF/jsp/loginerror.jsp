<%@page contentType="text/html" pageEncoding="UTF-8" isELIgnored="false"%>
<%@taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@taglib prefix="t" tagdir="/WEB-INF/tags" %>
<t:master title="Ryyppy.net - Virhe sisäänkirjautumisessa!">
    <jsp:attribute name="customHead">
        <link rel="stylesheet" type="text/css" href="/static/css/login.css" />
    </jsp:attribute>
    <jsp:body>
        <div id="apLogo"></div>
    
        <div id="login">
            <h2>Virhe sisäänkirjautumisessa!</h2>
            <p>Mahdollisia syitä:</p>
            <ul>
                <li>kirjoitit OpenId-tunnuksesi väärin</li>
                <li>OpenID-palveluntarjoajasi ei ole OpenID 2.0 -standardin mukainen</li>
                <li>tyrit kirjautumisesi OpenID-palveluntarjoajasi kanssa</li>
                <li>järjestelmässä on bugi, <a href="mailto:info@ryyppy.net">kerro siitä meille</a></li>
                <li>töhötit jotain muuta</li>
            </ul>
            <p>Jatka <a href="/ui/checklogin">etusivulle</a> ja kokeile uudelleen.</p>
        </div>
    </jsp:body>
</t:master>
