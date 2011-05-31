<%@page contentType="text/html" pageEncoding="UTF-8" isELIgnored="false"%>
<%@taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@taglib prefix="t" tagdir="/WEB-INF/tags" %>
<%@taglib uri="http://www.springframework.org/tags" prefix="spring"%>
<t:master title="Uusi juoja">
    <jsp:attribute name="customHead">
        <script type="text/javascript" src="/static/js/common.js"></script>
        <script type="text/javascript" src="/static/js/drinkerchecks.js"></script>
        <script type="text/javascript">
            $(document).ready(function() {
                openDialog($("#addDrinkerDialog"));
                $("#drinkerName").focus();
                
                $('.userField').live('keyup', function() { checkDrinkerFields(true); });
                $('#email').live('keyup', function() { checkEmail($(this).val()); });
                
                repaint();
            });
            
            $(window).resize(function() {
                repaint();
            });
            
            function repaint() {
                var windowWidth = $(window).width();
                var bestWidth = Math.min(600, windowWidth - 20);
                $("#registration").width(bestWidth);
            }
        </script>
    </jsp:attribute>
    <jsp:body>
        <div id="header">
            <h1><spring:message code="newuser.welcome" /></h1>
        </div>

        <div id="container">
            <div id="registration" class="dialog marginAuto">
                <h2><spring:message code="newuser.title" /></h2>

                <p><spring:message code="newuser.first_timer" /></p>

                <form method="post" action="addUser">
                    <label for="drinkerName"><spring:message code="form.name" /></label><br />
                    <input id="drinkerName" class="userField" type="text" name="name" /><br />

                    <label for="email"><spring:message code="form.email" /></label><br />
                    <input id="email" type="email" name="email" onblur="checkEmail($(this).val()); checkDrinkerFields(true);" onkeyup="checkEmail($(this).val()); checkDrinkerFields(true);" />
                    <span id="emailCorrect">&nbsp;</span><br />

                    <label for="sex"><spring:message code="form.sex" /></label><br />
                    <select name="sex">
                        <option value="MALE"><spring:message code="form.male" /></option>
                        <option value="FEMALE"><spring:message code="form.female" /></option>
                    </select><br />

                    <label for="drinkerWeight"><spring:message code="form.weight" /></label><br />
                    <input id="drinkerWeight" class="userField" type="password" name="weight" autocomplete="off" /><br />

                    <input id="submitButton" type="submit" value="<spring:message code="form.add_drinker" />" />
                </form>
            </div>
        </div>
    </jsp:body>
</t:master>