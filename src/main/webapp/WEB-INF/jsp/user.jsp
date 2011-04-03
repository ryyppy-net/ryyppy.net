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
        
        <!-- hack -->
        <!--[if lte IE 8]><script language="javascript" type="text/javascript" src="/static/js/flot/excanvas.min.js"></script><![endif]-->
        <script type="text/javascript" src="/static/js/flot/jquery.flot.min.js"></script>
        <script type="text/javascript" src="/static/js/flot/jquery.flot.crosshair.min.js"></script>
        <script type="text/javascript" src="/static/js/flot/jquery.flot.resize.min.js"></script>
        <script type="text/javascript" src="/static/js/userbutton.js"></script>
        
        <script type="text/javascript">
            var userButton = null;
            
            $(document).ready(function() {
                //fix_the_fucking_css();

                $('.party').each(function(index) {
                    $(this).css('background-color', colors[index]);
                });
                
                userButton = new UserButton(<c:out value="${user.id}" />, $('.userButton'), getColorAtIndex(0));
                userButton.update();
                //setInterval(function() {userButton.update()}, 1000);
            });
        </script>
        <title>Bileet</title>
    </head>
    <body>
        <div class="header">
            <a href="logout" onClick="return confirm('Haluatko varmasti kirjautua ulos?');">
                <div class="headerButton headerButtonLeft" id="goBack">
                    &lt;
                </div>
            </a>
            <div class="headerTextDiv">
                <h1><c:out value="${user.name}" /></h1>
            </div>
        </div>
        
        <!-- stupid css not able to center vertically properly -->
        <table style="height:200px; width:99%; margin-left:auto; margin-right:auto; margin-top:10px; margin-bottom:10px;">
            <tr style="width:100%;">
                <td class="userButton"></td>
            </tr>
        </table>
        
        <div class="header">
            <a href="#" onClick="openAddDrinkerPopupDialog();">
                <div class="headerButton headerButtonRight" id="addDrinkerButton">+</div>
            </a>
            <div class="headerTextDiv">
                <h1>Sun bileet</h1>
            </div>
        </div>

        <div id="body">
            <c:forEach items="${parties}" var="party">
                <c:url var="viewPartyUrl" value="partytouch?id=${party.id}" />

                <a href="${viewPartyUrl}">
                    <div class="party">
                        <span style="margin: 5px;">
                            <c:out value="${party.id}" />
                        </span>
                    </div>
                </a>
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
