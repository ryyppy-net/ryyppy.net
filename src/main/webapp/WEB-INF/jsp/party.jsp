<%@page contentType="text/html" pageEncoding="UTF-8" isELIgnored="false"%>
<%@taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@taglib prefix="t" tagdir="/WEB-INF/tags" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<%@taglib uri="http://www.springframework.org/tags" prefix="spring"%>
<t:master title="${party.name}">
    <jsp:attribute name="customHead">
        <link rel="stylesheet" href="/static/css/jquery.tooltip.css" type="text/css" media="screen" />
        
        <meta name="viewport" content="initial-scale=1.0, maximum-scale=1.0, user-scalable=0;">
        
        <script type="text/javascript">
            // global urls
            var dataUrl = '/API/parties/${party.id}/';
        </script>
        <script type="text/javascript" src="/static/js/jquery.tooltip.min.js"></script>
        
        <!-- hack -->
        <!--[if lte IE 8]><script language="javascript" type="text/javascript" src="/static/js/flot/excanvas.min.js"></script><![endif]-->
        <script type="text/javascript" src="/static/js/flot/jquery.flot.min.js"></script>
        <script type="text/javascript" src="/static/js/flot/jquery.flot.crosshair.min.js"></script>
        <script type="text/javascript" src="/static/js/flot/jquery.flot.resize.min.js"></script>
        
        <script type="text/javascript" src="/static/js/common.js"></script>
        <script type="text/javascript" src="/static/js/userbutton.js"></script>
        <script type="text/javascript" src="/static/js/drinkerchecks.js"></script>
        <script type="text/javascript" src="/static/js/partygraph.js"></script>
        <script type="text/javascript" src="/static/js/party.js"></script>

        <script type="text/javascript">
            function graphDialogOpened() {
                graphVisible = true;
                var element = $('#groupGraph');
                var dialog = $('#graphDialog');
                element.css('width', dialog.css('width')).css('height', (parseInt(dialog.css('height'), 0) - 10) + 'px');
                
                if (graph == null) {
                    var users = [];
<c:forEach items="${party.participants}" var="participant">
                    users.push({name: '${participant.name}', id:'${participant.id}'});
</c:forEach>
                    graph = new GroupGraph(users, element);
                }
                graph.options.legend = { position:'nw' };
                updateGroupGraph();
                if (!graphInterval)
                    graphInterval = setInterval(updateGroupGraph, updateInterval);
            }
        </script>
    </jsp:attribute>
    <jsp:body>
        <div class="header">
            <a class="headerButtonA" title="<spring:message code="party.tooltip.back"/>" href="/ui/user"><div class="headerButton headerButtonLeft" id="goBack"></div></a>
            <a id="graphButtonLink" class="headerButtonA" title="<spring:message code="party.tooltip.show_graph"/>" href="#"><div class="headerButton headerButtonLeft" id="graphButton"></div></a>
            <a id="addDrinkerButtonLink" class="headerButtonA" title="<spring:message code="party.tooltip.add_drinker"/>" href="#"><div class="headerButton headerButtonRight" id="addDrinkerButton"></div></a>
            <a id="kickDrinkerButtonLink" class="headerButtonA" title="<spring:message code="party.tooltip.remove_drinker"/>" href="#"><div class="headerButton headerButtonRight" id="kickDrinkerButton"></div></a>
            <div class="headerTextDiv">
                <h1 id="topic"><c:out value="${party.name}" /></h1>
            </div>
        </div>

        <div id="body">
            <table id="drinkers"></table>
        </div>
            
        <div id="graphDialog">
            <span class="closeButton"><a id="closeGraphDialogButton" href="#">X</a></span>
            <div style="margin-top: 13px;" id="groupGraph"></div>
        </div>

        <div id="addDrinkerDialog" class="popupDialog">
            <span class="closeButton"><a id="closeAddDrinkerDialogButton" href="#">X</a></span>

            <h2><spring:message code="party.add_drinker.registered"/></h2>
            <form method="post" action="<c:url value="linkUserToParty" />">
                <input type="hidden" name="partyId" value="${party.id}" />
                <input type="hidden" id="userId" name="userId" />
                
                <table>
                    <tr>
                        <th><spring:message code="form.email"/></th>
                        <td>
                            <input id="emailInput" type="text" name="email" onkeyup="getIdByEmail($(this).val(), '<c:out value="${party.id}" />');" onblur="getIdByEmail($(this).val(), '<c:out value="${party.id}" />');"/>
                        </td>
                        <td id="emailCorrect"></td>
                    </tr>
                    <tr>
                        <th>&nbsp;</th>
                        <td colspan="2"><input id="linkUserButton" type="submit" value="<spring:message code="form.add_drinker"/>" disabled="disabled" onClick="forceRefresh();" /></td>
                    </tr>
                </table>
            </form>

            <h2><spring:message code="party.add_drinker.guest"/></h2>
            <form method="post" action="<c:url value="addAnonymousUser" />">
                <input type="hidden" name="partyId" value="${party.id}" />
                <table>
                    <tr>
                        <th><spring:message code="form.name"/></th>
                        <td><input id="drinkerName" type="text" name="name" onkeyup="checkDrinkerFields(false);" /></td>
                    </tr>
                    <tr>
                        <th><spring:message code="form.sex"/></th>
                        <td>
                            <select name="sex">
                                <option value="MALE"><spring:message code="form.male"/></option>
                                <option value="FEMALE"><spring:message code="form.female"/></option>
                            </select>
                        </td>
                    </tr>
                    <tr>
                        <th><spring:message code="form.weight"/></th>
                        <td><input id="drinkerWeight" type="password" name="weight" onkeyup="checkDrinkerFields(false);" autocomplete="off" /></td>
                    </tr>
                    <tr>
                        <th>&nbsp;</th>
                        <td><input id="submitButton" type="submit" value="<spring:message code="form.add_drinker"/>" onClick="forceRefresh();" disabled="disabled" /></td>
                    </tr>
                </table>
            </form>
        </div>
                
        <div id="kickDrinkerDialog" class="popupDialog">
            <span class="closeButton"><a id="closeKickDrinkerDialogButton" href="#">X</a></span>

            <h2><spring:message code="party.remove_drinker"/></h2>
            <ul>
                <c:choose>
                    <c:when test="${fn:length(party.participants) < 2}">
                        <li><spring:message code="party.no_users"/></li>
                    </c:when>
                    <c:otherwise>
                        <c:forEach var="participant" items="${party.participants}">
                            <c:url var="leavePartyUrl" value="removeUserFromParty?partyId=${party.id}&userId=${participant.id}" />
                            <c:if test="${participant.id != user.id}">
                                <li>
                                    <a href="#" onClick="if (confirm('<spring:message code="party.confirm.remove_user"/> ${participant.name} <spring:message code="party.confirm.from_party"/>'))
                                        $.get('${leavePartyUrl}', function() {location.reload(true);});">
                                        <c:out value="${participant.name}" /> 
                                    </a>
                                </li>
                            </c:if>
                        </c:forEach>
                    </c:otherwise>
                </c:choose>
            </ul>
        </div>
    </jsp:body>
</t:master>

