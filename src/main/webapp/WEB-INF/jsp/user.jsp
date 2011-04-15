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
        <script type="text/javascript" src="/static/js/drinkerchecks.js"></script>
        
        <script type="text/javascript">
            var userButton = null;
            
            $(document).ready(function() {
                //fix_the_fucking_css();

                $('.party').each(function(index) {
                    $(this).css('background-color', colors[index]);
                });
                
                userButton = new UserButton(<c:out value="${user.id}" />, $('.userButton'), getColorAtIndex(0));
                userButton.update();
                setInterval(function() {userButton.update()}, 60 * 1000);
            });
        </script>
        <title>Bileet</title>
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
        <div class="header">
            <a href="logout" onClick="return confirm('Haluatko varmasti kirjautua ulos?');">
                <div class="headerButton headerButtonLeft" id="goBack">
                </div>
            </a>
            <a href="#" onClick="toggleDialog($('#configureDrinkerDialog'), checkEmail);">
                <div class="headerButton headerButtonRight" id="configureButton">
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
            <a href="#" onClick="toggleDialog($('#addDrinkerDialog'));">
                <div class="headerButton headerButtonRight" id="addDrinkerButton"></div>
            </a>
            <div class="headerTextDiv">
                <h1>Sun bileet</h1>
            </div>
        </div>

        <div id="body">
            <c:forEach items="${parties}" var="party">
                <c:url var="viewPartyUrl" value="partytouch?id=${party.id}" />
                <c:url var="leavePartyUrl" value="removeUserFromParty?partyId=${party.id}&userId=${user.id}" />

                <div class="party">
                    <a href="${viewPartyUrl}">
                        <span style="margin: 5px;">
                            <c:out value="${party.name}" />
                        </span>
                    </a>
                    <a onClick="return confirm('Haluatko varmasti lähteä bileistä?');" href="${leavePartyUrl}">
                        <img src="/static/images/x.png" style="float:right; margin-right: 5px;" />
                    </a>
                    </div>
            </c:forEach>
        </div>

        <div id="addDrinkerDialog" class="popupDialog">
            <span style="float: right;"><a href="#" onClick="closeDialog($('#addDrinkerDialog'));">X</a></span>

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
                
        <div id="configureDrinkerDialog" class="popupDialog">
            <span style="float: right;"><a href="#" onClick="closeDialog($('#configureDrinkerDialog'));">X</a></span>

            <h2>Muokkaa tietojasi</h2>

            <form method="post" action="<c:url value="modifyUser" />">
                <input type="hidden" name="userId" value="${user.id}" />                
                <table>
                    <tr>
                        <th>Nimi</th>
                        <td colspan="2"><input id="drinkerName" type="text" name="name" onchange="checkDrinkerFields(true);" value="<c:out value='${user.name}' />" /></td>
                    </tr>
                    <tr>
                        <th>Sähköposti</th>
                        <td><input id="email" type="text" name="email" value="<c:out value='${user.email}' />" onchange="checkEmail($(this).val(), '<c:out value='${user.email}'/>'); checkDrinkerFields(true);" /></td>
                        <td style="width:24px;height:24px;" id="emailCorrect">&nbsp;</td>
                    </tr>
                    <tr>
                        <th>Sukupuoli</th>
                        <td colspan="2">
                            <select name="sex">
                                <option <c:if test="${user.sex == 'MALE'}">selected="selected"</c:if> value="MALE">Mies</option>
                                <option <c:if test="${user.sex == 'FEMALE'}">selected="selected"</c:if> value="FEMALE">Nainen</option>
                            </select>
                        </td>
                    </tr>
                    <tr>
                        <th>Paino</th>
                        <td colspan="2"><input value="<c:out value='${user.weight}' />" id="drinkerWeight" type="password" name="weight" onchange="checkDrinkerFields(true);" /></td>
                    </tr>
                    <tr>
                        <th>&nbsp;</th>
                        <td colspan="2"><input id="submitButton" type="submit" value="Tallenna" disabled="disabled" /></td>
                    </tr>
                </table>
            </form>
        </div>
    </body>
</html>
