<%@page contentType="text/html" pageEncoding="UTF-8" isELIgnored="false"%>
<%@taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN"
   "http://www.w3.org/TR/html4/loose.dtd">

<html>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
        <title>Ryyppy.net - Virhe sisäänkirjautumisessa!</title>
        <link rel="stylesheet" type="text/css" href="/static/css/style.css" />
        <link rel="stylesheet" type="text/css" href="/static/css/login.css" />
 
        <script type="text/javascript" src="/static/js/jquery.js"></script>
        <script type="text/javascript">

          var _gaq = _gaq || [];
          _gaq.push(['_setAccount', 'UA-22483744-1']);
          _gaq.push(['_trackPageview']);

          (function() {
            var ga = document.createElement('script'); ga.type = 'text/javascript'; ga.async = true;
            ga.src = ('https:' == document.location.protocol ? 'https://ssl' : 'http://www') + '.google-analytics.com/ga.js';
            var s = document.getElementsByTagName('script')[0]; s.parentNode.insertBefore(ga, s);
          })();

        </script>
    </head>

<body>
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
</body>
</html>
