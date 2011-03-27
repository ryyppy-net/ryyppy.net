<%@page contentType="text/html" pageEncoding="UTF-8" isELIgnored="false"%>
<%@taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN"
   "http://www.w3.org/TR/html4/loose.dtd">
<!doctype html>
<html>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
        <link rel="stylesheet" href="/static/css/style.css" type="text/css" media="screen" />
        <script type="text/javascript" src="/static/js/jquery.js"></script>
        <script type="text/javascript" src="/static/js/common.js"></script>
        <script type="text/javascript">
            $(document).ready(function() {
                fix_the_fucking_css();

                $('.party').each(function(index) {
                    $(this).css('background-color', colors[index]);
                });
            });
        </script>
        <title>Bileet</title>
    </head>
    <body>
        <div id="header">
            <h1>Bileet (<c:out value="${user.name}" />)</h1>
            <a href="logout" onClick="return confirm('Haluatko varmasti kirjautua ulos?');"><div id="goBack">&lt;</div></a>
            <a href="#" onClick="openPopupDialog();"><div id="addDrinkerButton">+</div></a>
        </div>

        <div id="body">
            <c:forEach items="${parties}" var="party">
                <div class="party">
                    <span style="margin: 5px;">
                        <c:url var="viewPartyUrl" value="viewParty?id=${party.id}" />
                        <a href="${viewPartyUrl}"><c:out value="${party.id}" /></a>
                    </span>
                </div>
            </c:forEach>
        </div>

        <div id="addDrinkerDialog">
            <span style="float: right;"><a href="#" onClick="closeAddDrinkerDialog();">X</a></span>

            <h2>Uudet bileet</h2>

            <form method="POST" action="<c:url value="addParty" />">
                <input type="hidden" name="userId" value="${user.id}" />
                <table>
                    <tr>
                        <th>Nimi</th><td><input type="text" name="name" /></td>
                    </tr>
                    <tr>
                        <th>&nbsp;</th><td><input type="submit" value="Luo bileet" /></td>
                    </tr>
                </table>
            </form>
        </div>
    </body>
</html>
