<%-- 
    Document   : jQueryTemplates
    Created on : Jun 5, 2011, 3:03:57 PM
    Author     : thardas
--%>

<%@tag description="jQueryTemplates" pageEncoding="UTF-8"%>

<script id="datetimepickerTemplate" type="text/x-jquery-tmpl">
    <form class="datetimepicker" method="post" action="#">
        <table>
            <tr>
                <td><input id="increaseDays" type="submit" value="+" /></td>
                <td><input id="increaseMonths" type="submit" value="+" /></td>
                <td><input id="increaseYears" type="submit" value="+" /></td>
            </tr>
            <tr>
                <td><input id="dateDay" type="text" value="1" size="2" /></td>
                <td><input id="dateMonth" type="text" value="1" size="2" /></td>
                <td><input id="dateYear" type="text" value="2011" size="4" /></td>
            </tr>
            <tr>
                <td><input id="decreaseDays" type="submit" value="-" /></td>
                <td><input id="decreaseMonths" type="submit" value="-" /></td>
                <td><input id="decreaseYears" type="submit" value="-" /></td>
            </tr>
        </table>

        <table>
            <tr>
                <td><input id="increaseHours" type="submit" value="+" /></td>
                <td><input id="increaseMinutes" type="submit" value="+" /></td>
            </tr>
            <tr>
                <td><input id="dateHour" type="text" value="00" size="2" /></td>
                <td><input id="dateMinute" type="text" value="00" size="2" /></td>
            </tr>

            <tr>
                <td><input id="decreaseHours" type="submit" value="-" /></td>
                <td><input id="decreaseMinutes" type="submit" value="-" /></td>
            </tr>
        </table>
    </form>
</script>
