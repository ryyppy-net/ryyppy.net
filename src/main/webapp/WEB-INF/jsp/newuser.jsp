<%@page contentType="text/html" pageEncoding="UTF-8" isELIgnored="false"%>
<%@taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@taglib prefix="t" tagdir="/WEB-INF/tags" %>
<%@taglib uri="http://www.springframework.org/tags" prefix="spring"%>
<t:master title="Uusi juoja">
    <jsp:attribute name="customHead">
        <script type="text/javascript" src="/static/js/common.js"></script>
        <script type="text/javascript" src="/static/js/drinkerchecks.js"></script>
        
        <link rel="stylesheet" href="/static/angular/app/css/bootstrap.css"/>
        <link rel="stylesheet" href="/static/angular/app/css/app.css"/>
        
        <link href="http://fonts.googleapis.com/css?family=Rum+Raisin&subset=latin,latin-ext" rel="stylesheet" type="text/css" />
        
        <script type="text/javascript">
            $(document).ready(function() {
                $("#drinkerName").focus();
                
                $('.userField').live('keyup', function() { checkDrinkerFields(true); });
                $('#email').live('keyup', function() { checkEmail($(this).val()); });
            });
        </script>
    </jsp:attribute>
    <jsp:body>
        <div class="navbar">
            <div class="navbar-inner">
                <span class="brand">Ryyppy.net</span>
                <ul class="nav pull-right">
                    <li><a href="/">Takaisin etusivulle</a></li>
                </ul>
            </div>
        </div>

        <div class="container-fluid">
            <div class="row-fluid">
                <div class="offset1 span10">
                    <h1>Tervetuloa!</h1>
                    <p>Olet näemmä ensimmäistä kertaa tällä sivulla. Ole hyvä ja täytä allaolevat tiedot, niin aletaan ryyppäämään!</p>

                    <form class="form-horizontal" method="post" action="/ui/addUser">
                        <div class="control-group">
                            <label class="control-label" for="drinkerName">Nimi</label>
                            <div class="controls">
                                <input id="drinkerName" class="userField" type="text" name="name" />
                            </div>
                        </div>

                        <div class="control-group">
                            <label class="control-label" for="email">Sähköposti</label>
                            <div class="controls">
                                <input id="email" type="email" name="email" onblur="checkEmail($(this).val()); checkDrinkerFields(true);" onkeyup="checkEmail($(this).val()); checkDrinkerFields(true);" />
                                <span id="emailCorrect">&nbsp;</span><br />
                            </div>
                        </div>

                        <div class="control-group">
                            <label class="control-label" for="sex">Sukupuoli</label>
                            <div class="controls">
                                <select id="sex" name="sex">
                                    <option value="MALE">Mies</option>
                                    <option value="FEMALE">Nainen</option>
                                </select>
                            </div>
                        </div>

                        <div class="control-group">
                            <label class="control-label" for="drinkerWeight">Paino</label>
                            <div class="controls">
                                <input id="drinkerWeight" class="userField" type="password" name="weight" autocomplete="off" />
                            </div>
                        </div>

                        <div class="control-group">
                            <div class="controls">
                                <input id="submitButton" class="btn btn-success" type="submit" value="Rekisteröidy!" />
                            </div>
                        </div>
                    </form>
                </div>
            </div>
        </div>
    </jsp:body>
</t:master>