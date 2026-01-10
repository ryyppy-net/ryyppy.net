<%@page contentType="text/html" pageEncoding="UTF-8" isELIgnored="false"%>
<%@taglib uri="jakarta.tags.core" prefix="c" %>
<%@taglib prefix="t" tagdir="/WEB-INF/tags" %>
<%@ taglib uri="jakarta.tags.functions" prefix="fn" %>
<%@taglib uri="http://www.springframework.org/tags" prefix="spring"%>
<t:master title="${party.name}">
    <jsp:attribute name="customHead">
        <link rel="stylesheet" href="/static/css/jquery-ui/jquery-ui-1.8.12.custom.css" type="text/css" media="screen" />
        
        <!-- hack -->
        <!--[if lte IE 8]><script language="javascript" type="text/javascript" src="/static/js/flot/excanvas.min.js"></script><![endif]-->
        <script type="text/javascript" src="/static/js/flot/jquery.flot.min.js"></script>
        <script type="text/javascript" src="/static/js/flot/jquery.flot.crosshair.min.js"></script>
        <script type="text/javascript" src="/static/js/flot/jquery.flot.resize.min.js"></script>
        <script type="text/javascript" src="/static/js/jquery-ui-1.8.12.custom.min.js"></script>
        <script type="text/javascript" src="/static/js/jquery.tmpl.js"></script>
        
        <script type="text/javascript" src="/static/js/common.js"></script>
        <script type="text/javascript" src="/static/js/userbutton.js"></script>
        <script type="text/javascript" src="/static/js/userbuttongrid.js"></script>
        <script type="text/javascript" src="/static/js/drinkerchecks.js"></script>
        <script type="text/javascript" src="/static/js/partygraph.js"></script>
        <script type="text/javascript" src="/static/js/party.js"></script>

        <script type="text/javascript">
            var partyId = ${party.id};
            
            var partyHost = new PartyHost(partyId);
            partyHost.onUpdate = pageUpdate;
            var grid = new UserButtonGrid($('#drinkers'));
            grid.onUserDrunk = onUserDrunk;

            $(document).ready(function() {
                $('#addDrinkerAccordion').accordion({
                    fillSpace: true
                });
                
                $('#addDrinkerDialog').dialog({ width: 600, autoOpen: false, draggable: false, resizable: false });
                $('#kickDrinkerDialog').dialog({ width: 600, autoOpen: false, draggable: false, resizable: false });
                $('#groupGraphDialog').dialog({
                    autoOpen: false,
                    draggable: false,
                    resizable: false,
                    open: function() {
                        graphDialogOpened();
                    }
                });
            });
            
            function addAnonymousUser() {
                RyyppyAPI.addAnonymousUserToParty(partyId, $('#drinkerName').val(), $('#drinkerSex').val(), $('#drinkerWeight').val(),
                    function() {
                        partyHost.update();
                        $('#addDrinkerDialog').dialog('close');
                    },
                    function() { alert('fail'); }
                );
                return false;
            }
            
            function linkUserToParty() {
                RyyppyAPI.linkUserToParty(partyId, $('#userId').val(),
                    function() {
                        partyHost.update();
                        $('#addDrinkerDialog').dialog('close');
                    },
                    function() { alert('fail'); }
                );
                return false;
            }
        </script>
    </jsp:attribute>
    <jsp:body>
        <div class="header">
            <table style="width: 100%;" class="noBorders">
                <tr>
                    <td class="headerButton"><a class="headerButtonA" title="<spring:message code="party.tooltip.back"/>" href="/ui/user"><div id="goBack" class="headerButton headerButtonLeft"></div></a></td>
                    <td class="headerButton"><a id="graphButtonLink" class="headerButtonA" title="<spring:message code="party.tooltip.show_graph"/>" href="#"><div id="graphButton" class="headerButton headerButtonLeft"></div></a></td>
                    <td class="topic"><h1 class="topic" id="topic">&nbsp;</h1></td>                    
                    <td class="headerButton"><a id="addDrinkerButtonLink" class="headerButtonA" title="<spring:message code="party.tooltip.add_drinker"/>" href="#"><div id="addDrinkerButton" class="headerButton headerButtonRight"></div></a></td>
                    <td class="headerButton"><a id="kickDrinkerButtonLink" class="headerButtonA" title="<spring:message code="party.tooltip.remove_drinker"/>" href="#"><div id="kickDrinkerButton" class="headerButton headerButtonRight"></div></a></td>
                </tr>
            </table>
        </div>

        <div id="body">
            <table id="drinkers"></table>
        </div>
            
        <div id="groupGraphDialog" class="popupDialog" title="<spring:message code="party.group_graph"/>">
            <div id="groupGraph"></div>
        </div>

        <div id="addDrinkerDialog" class="popupDialog" title="<spring:message code="party.tooltip.add_drinker"/>">
            <div id="addDrinkerAccordion">
                <h2><a href="#"><spring:message code="party.add_drinker.registered"/></a></h2>
                <div>
                    <form method="post" action="#">
                        <input type="hidden" name="partyId" value="${party.id}" />
                        <input type="hidden" id="userId" name="userId" />

                        <table>
                            <tr>
                                <th><spring:message code="form.email"/></th>
                                <td>
                                    <input id="emailInput" type="email" name="email" onkeyup="getIdByEmail($(this).val(), '<c:out value="${party.id}" />');" onblur="getIdByEmail($(this).val(), '<c:out value="${party.id}" />');"/>
                                </td>
                                <td id="emailCorrect"></td>
                            </tr>
                            <tr>
                                <th>&nbsp;</th>
                                <td colspan="2"><input id="linkUserButton" type="submit" value="<spring:message code="form.add_drinker"/>" disabled="disabled" onClick="return linkUserToParty();" /></td>
                            </tr>
                        </table>
                    </form>
                </div>

                <h2><a href="#"><spring:message code="party.add_drinker.guest"/></a></h2>
                <div>
                    <form method="post" action="#">
                        <input type="hidden" name="partyId" value="${party.id}" />
                        <table>
                            <tr>
                                <th><spring:message code="form.name"/></th>
                                <td><input id="drinkerName" type="text" name="name" onkeyup="checkDrinkerFields(false);" /></td>
                            </tr>
                            <tr>
                                <th><spring:message code="form.sex"/></th>
                                <td>
                                    <select id="drinkerSex" name="sex">
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
                                <td><input id="submitButton" type="submit" value="<spring:message code="form.add_drinker"/>" onClick="return addAnonymousUser();" disabled="disabled" /></td>
                            </tr>
                        </table>
                    </form>
                </div>
            </div>
        </div>
                
        <div id="kickDrinkerDialog" class="popupDialog" title="<spring:message code="party.remove_drinker"/>">
            <c:choose>
                <c:when test="${fn:length(party.participants) < 2}">
                    <p><spring:message code="party.no_users"/>
                </c:when>
                <c:otherwise>
                    <ul>
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
                    </ul>
                </c:otherwise>
            </c:choose>
        </div>
    </jsp:body>
</t:master>

