<%@page contentType="text/html" pageEncoding="UTF-8" isELIgnored="false"%>
<%@taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@taglib prefix="t" tagdir="/WEB-INF/tags" %>
<%@taglib uri="http://www.springframework.org/tags" prefix="spring"%>
<t:master>
    <jsp:attribute name="customHead">
        <link rel="stylesheet" href="/static/css/jquery-ui/jquery-ui-1.8.12.custom.css" type="text/css" media="screen" />
        
        <script type="text/javascript" src="/static/js/common.js"></script>
        <script type="text/javascript" src="/static/js/datetimepicker.js"></script>
        <script type="text/javascript" src="/static/js/jquery-ui-1.8.12.custom.min.js"></script>
        <script type="text/javascript" src="/static/js/jquery-ui-timepicker-addon.js"></script>
        <script type="text/javascript" src="/static/js/jquery.ui.datepicker-fi.js"></script>
        
        <!-- hack -->
        <!--[if lte IE 8]><script language="javascript" type="text/javascript" src="/static/js/flot/excanvas.min.js"></script><![endif]-->
        <script type="text/javascript" src="/static/js/flot/jquery.flot.min.js"></script>
        <script type="text/javascript" src="/static/js/flot/jquery.flot.crosshair.min.js"></script>
        <script type="text/javascript" src="/static/js/flot/jquery.flot.resize.min.js"></script>
        <script type="text/javascript" src="/static/js/userbutton.js"></script>
        <script type="text/javascript" src="/static/js/drinkerchecks.js"></script>
        <script type="text/javascript" src="/static/js/userhistorygraph.js"></script>
        <script type="text/javascript" src="/static/js/date.js"></script>
        <script type="text/javascript" src="/static/js/jquery.tmpl.js"></script>

        <script type="text/javascript">
            var userButton = null;
            var userId = ${user.id};
            var picker;
            
            $(document).ready(function() {
                $('.party').each(function(index) {
                    $(this).css('background-color', colors[index]);
                });
                
                userButton = new UserButton(<c:out value="${user.id}" />, $('.userButton'), getColorAtIndex(0));
                userButton.onDataLoaded = onButtonDataUpdated;
                userButton.update();
                setInterval(function() {userButton.update()}, 60 * 1000);

                var user = { id: ${user.id}, name: '${user.name}' };
                
                var element = $('#historyGraph');
                var graph = new UserHistoryGraph(user, element);
                graph.options.legend = { position:'nw' };
                graph.update();
            });

            function onButtonDataUpdated() {
                if (userButton.series == null) return;

                var max = 0;
                for (var j in userButton.series[0].data) {
                    var d = userButton.series[0].data[j];
                    var a = d[1];
                    if (Number(a) >= Number(max))
                        max = a;
                }
                max = Math.floor(max) + 1;
                userButton.setMaxY(max);
            }

            function configureDrinksDialogOpened() {
                RyyppyAPI.getUserDrinks(${user.id}, gotDrinkData);
                $("#time").datetimepicker($.datepicker.regional['fi']);
                $("#time").datetimepicker("option", {maxDate: 0});
                $("#time").datetimepicker("setDate", new Date());
            }

            function configureDrinksDialogClosed() {
                location.reload(true);
            }

            function formatDate(date) {
                return date.toString('yyyy-MM-dd HH:mm');
            }

            function gotDrinkData(data) {
                $("#drinksList").html("");
                var drinkData = [];
                var drinkingTimeText = '<spring:message code="user.drink_time"/>';

                $(data).find('drink').each(function() {
                    var id = $(this).find('id').text();
                    var timestamp = Number($(this).find('timestamp').text());
                    var date = formatDate(new Date(timestamp));
                    drinkData.push({DrinkId: id, DrinkingTimeText: drinkingTimeText, DrinkingTime: date, UserId: userId});
                });
                
                if (drinkData.length == 0) {
                    var li = $('<li>').html('<spring:message code="user.no_drinks"/>');
                    $("#drinksList").append(li);
                }
                else {
                    $.get('/static/templates/drinkListElement.html', function(template) {
                        $.tmpl(template, drinkData).appendTo('#drinksList');
                        
                        $('.drink').unbind('click');
                        $('.drink').bind('click', function() {
                            var element = $.tmplItem(this);
                            if (confirm('<spring:message code="user.remove_drink.confirm"/>'))
                                $.get('/ui/removeDrink?userId=' + element.data.UserId + '&drinkId=' + element.data.DrinkId, configureDrinksDialogOpened);
                        });
                    });
                }
            }

            $(document).ready(function() {
                $('#addPartyButtonLink').click(function() {
                    toggleJQUIDialog($('#addPartyDialog'));
                });
                $('#configureDrinkerButtonLink').click(function() {
                    toggleJQUIDialog($('#configureDrinkerDialog'));
                });
                $('#configureDrinksButtonLink').click(function() {
                    toggleJQUIDialog($('#configureDrinksDialog'));
                    $("#configureDrinksAccordion").accordion({ fillSpace: true });
                });
                
                $('#addPartyDialog').dialog({ width: 600, autoOpen: false, draggable: false, resizable: false });                
                $('#configureDrinkerDialog').dialog({ width: 600, autoOpen: false, draggable: false, resizable: false });
                
                
                
                $('#submitTime').click(function() {
                    var formattedTime = '{0}.{1}.{2} {3}:{4}'.format(
                        picker.selectedTime.getDate(),
                        picker.selectedTime.getMonth() + 1,
                        picker.selectedTime.getFullYear(),
                        picker.selectedTime.getHours(),
                        picker.selectedTime.getMinutes()
                    );
                    $('#date').val(formattedTime);
                });
                
                
                $('#configureDrinksDialog').dialog({
                    modal: true,
                    draggable: false,
                    autoOpen: false,
                    resizable: false,
                    width: 600,
                    
                    open: function() {
                        configureDrinksDialogOpened();
                        $("#configureDrinksAccordion").accordion({ fillSpace: true });
                        picker = new DateTimePicker('#historyDrinkTime');
                    },
                    
                    close: function() {
                        $('#historyDrinkTime').html('');
                    }
                });
                
                repaint();
            });
            
            $(window).resize(function() {
                repaint();
            });
            
            function repaint() {
                var windowWidth = $(window).width();
                var bestWidth = windowWidth - 15;
                $("#userButtonTable").width(bestWidth);
                $(".party").width(bestWidth);
                $("#historyGraph").width(bestWidth - 35);
                
                var bestSize = calculateBestDialogSize();
                resizePopupDialogs(bestSize);
            }
        </script>
    </jsp:attribute>
    <jsp:body>
        <div class="header">
            <table style="width: 100%;" class="noBorders">
                <tr>
                    <td class="headerButton"><a class="headerButtonA" title="<spring:message code="user.logout_dialog.title"/>" href="logout" onClick="return confirm('<spring:message code="user.logout_dialog.msg"/>');"><div class="headerButton headerButtonLeft" id="goBack"></div></a></td>
                    <td class="topic"><h1 class="topic"><c:out value="${user.name}" /></h1></td>
                    <td class="headerButton"><a id="configureDrinkerButtonLink" class="headerButtonA" title="<spring:message code="user.settings"/>" href="#"><div class="headerButton headerButtonRight" id="configureButton"></div></a></td>
                </tr>
            </table>
        </div>
        
        <!-- stupid css not able to center vertically properly -->
        <div id="body" class="body">
            <table id="userButtonTable">
                <tr>
                    <td class="userButton roundedCornersBordered"></td>
                </tr>
            </table>
        </div>
        
        <div class="header headerMargin">
            <a id="addPartyButtonLink" class="headerButtonA" title="<spring:message code="user.add_party"/>" href="#">
                <div id="addPartyButton" class="headerButton headerButtonRight"></div>
            </a>
            <div class="headerTextDiv">
                <h1><spring:message code="user.your_parties"/></h1>
            </div>
        </div>

        <div class="body">
            <c:forEach items="${parties}" var="party">
                <c:url var="viewPartyUrl" value="party?id=${party.id}" />
                <c:url var="leavePartyUrl" value="removeUserFromParty?partyId=${party.id}&userId=${user.id}" />

                <div class="party roundedCornersBordered">
                    <div class="marginBox">
                        <a href="${viewPartyUrl}">
                            <span>
                                <c:out value="${party.name}" />
                            </span>
                            <span id="partyStartTime">
                                <spring:message code="user.start_time"/> <script type="text/javascript">document.write(formatDate(new Date(<c:out value="${party.startTime.time}" />)));</script>
                            </span>
                        </a>
                            <a class="headerButtonA" title="<spring:message code="user.exit_dialog.title"/>" onClick="return confirm('<spring:message code="user.exit_dialog.msg"/>');" href="${leavePartyUrl}">
                            <img src="/static/images/x.png" alt="sulje" style="float:right; margin-right: 5px;" />
                        </a>
                    </div>
                </div>
            </c:forEach>
        </div>

        <div class="header headerMargin">
            <a id="configureDrinksButtonLink" class="headerButtonA" title="<spring:message code="user.add_or_remove_drinks"/>" href="#">
                <div class="headerButton headerButtonRight" id="configureDrinksButton"></div>
            </a>
            <div class="headerTextDiv">
                <h1><spring:message code="user.history"/></h1>
            </div>
        </div>

        <div class="body">
            <div id="historyGraph" class="roundedCornersBordered"></div>
        </div>
            
        <div id="configureDrinksDialog" class="popupDialog" title="<spring:message code="user.history"/>">
            <div id="configureDrinksAccordion">
                <h2><a href="#"><spring:message code="user.add_drinks_at"/></a></h2>
                <div>
                    <div id="historyDrinkTime"></div>
                    <form method="POST" action="<c:url value="addDrinkToDate" />">
                        <input type="hidden" name="userId" value="${user.id}" />
                        <input type="hidden" id="date" name="date" value="" />
                        <input type="submit" value="Lisää" id="submitTime" />
                    </form>
                </div>

                <h2><a href="#"><spring:message code="user.remove_drink"/></a></h2>
                <div>
                    <ul id="drinksList">
                    </ul>
                </div>
            </div>
        </div>

        <div id="addPartyDialog" class="popupDialog" title="<spring:message code="user.new_party"/>">
            <form method="POST" action="<c:url value="addParty" />">
                <input type="hidden" name="userId" value="${user.id}" />
                <table>
                    <tr>
                        <th><spring:message code="form.name"/></th>
                        <td><input id="nameInput" type="text" name="name" /></td>
                    </tr>
                    <tr>
                        <th>&nbsp;</th>
                        <td><input type="submit" value="<spring:message code="form.create_party"/>" /></td>
                    </tr>
                </table>
            </form>
        </div>
                
        <div id="configureDrinkerDialog" class="popupDialog" title="<spring:message code="user.edit"/>">
            <form method="post" action="<c:url value="modifyUser" />">
                <input type="hidden" name="userId" value="${user.id}" />                
                <table>
                    <tr>
                        <th><spring:message code="form.name"/></th>
                        <td colspan="2"><input id="drinkerName" type="text" name="name" onkeyup="checkDrinkerFields(true);" value="<c:out value='${user.name}' />" /></td>
                    </tr>
                    <tr>
                        <th><spring:message code="form.email"/></th>
                        <td><input id="email" type="email" name="email" value="<c:out value='${user.email}' />" onkeyup="checkEmail($(this).val(), '<c:out value='${user.email}'/>'); checkDrinkerFields(true);" /></td>
                        <td style="width:24px;height:24px;" id="emailCorrect">&nbsp;</td>
                    </tr>
                    <tr>
                        <th><spring:message code="form.sex"/></th>
                        <td colspan="2">
                            <select name="sex">
                                <option <c:if test="${user.sex == 'MALE'}">selected="selected"</c:if> value="MALE"><spring:message code="form.male"/></option>
                                <option <c:if test="${user.sex == 'FEMALE'}">selected="selected"</c:if> value="FEMALE"><spring:message code="form.female"/></option>
                            </select>
                        </td>
                    </tr>
                    <tr>
                        <th><spring:message code="form.weight"/></th>
                        <td colspan="2"><input value="<c:out value='${user.weight}' />" id="drinkerWeight" type="password" name="weight" onkeyup="checkDrinkerFields(true);" autocomplete="off" /></td>
                    </tr>
                    <tr>
                        <th>&nbsp;</th>
                        <td colspan="2"><input id="submitButton" type="submit" value="<spring:message code="form.save"/>" /></td>
                    </tr>
                </table>
            </form>
        </div>
    </jsp:body>
</t:master>