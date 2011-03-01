<%@page contentType="text/html" pageEncoding="UTF-8" isELIgnored="false"%>
<%@taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml" dir="ltr" lang="fi-FI">

    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
        <title>Alkoholilaskuri</title>
        <link rel="stylesheet" href="/static/css/style.css" type="text/css" media="screen" />
        <script type="text/javascript" src="/static/js/jquery.js"></script>
        <script type="text/javascript" src="/static/js/graph.js"></script>
        <script type="text/javascript">

            function get_param( name )
            {
                name = name.replace(/[\[]/,"\\\[").replace(/[\]]/,"\\\]");
                var regexS = "[\\?&]"+name+"=([^&#]*)";
                var regex = new RegExp( regexS );
                var results = regex.exec( window.location.href );
                if( results == null )
                    return "";
                else
                    return results[1];
            }

            var partyId = get_param('partyId');
            var addDrinkUrl = '/API/parties/${party.id}/add-drink?participant-id=';
            var historyUrl = '/API/parties/${party.id}/show-history';
            var dataUrl = '/API/parties/${party.id}/';

            $(document).ready(function() {
                get_data(foo);
                setInterval(function() { if (needsResizing == true) forceRefresh(); }, 1000);
                setInterval(function() { get_data(update); }, 5 * 60 * 1000);
            });

            function fix(i) { if(i < 16) { return 16; } if(i > 255) { return 255; } return i; }

            function vari() {
                var d = 500;
                var r = Math.random()*255;
                var g = Math.random()*255;
                var b = Math.random()*255;
                var sum = r+g+b;
                r /= (sum/d);
                g /= (sum/d);
                b /= (sum/d);
                return '#'+parseInt(fix(r)).toString(16)+parseInt(fix(g)).toString(16)+parseInt(fix(b)).toString(16);
            }

            function forceRefresh() {
                needsResizing = false;
                $('#drinkers').html('');
                get_data(foo);
            }

            function determine_layout(n) {
                var best = [0, 0];
                var initial = Math.ceil(Math.sqrt(n));
                var square_candidate = initial * initial;
                var other_candidate  = (initial - 1) * (initial + 1);

                if (Math.abs(square_candidate - n) < Math.abs(other_candidate - n)) {
                    best = [initial, initial];
                } else {
                    best = [initial - 1, initial + 1];
                }

                if ((best[0] * (best[1] - 1)) >= n) {
                    best[1] = best[1] - 1;
                }

                return best;
            }

            function pivot_layout_if_necessary(layout) {
                var layout_aspect = layout[0] < layout[1];
                var window_aspect = $(window).width() < $(window).height();
                if (layout_aspect != window_aspect) {
                    return [layout[1], layout[0]];
                }

                return layout;
            }

            function get_data(callback) {
                $.get(dataUrl, callback);
            }

            var needsResizing = false;

            $(window).resize(function() {
                needsResizing = true;
            });

            function update(data) {
                var newdata = parse_data(data);

                for (var i in persons) {
                    var person = persons[i];
                    var found = false;

                    for (var j in newdata) {
                        var d = newdata[j];
                        if (d.id == person.id) {
                            $('#' + person.id).html(getPersonHtml(d));
                            found = true;
                            break;
                        }
                    }
                    if (!found) {
                        forceRefresh();
                        return;
                    }
                }

                for (var j in newdata) {
                    var d = newdata[j];
                    var found = false;

                    for (var i in persons) {
                        var person = persons[i];
                        if (d.id == person.id) {
                            found = true;
                            break;
                        }
                    }
                    if (!found) {
                        forceRefresh();
                        return;
                    }
                }
            }

            function getPersonHtml(d) {
                return '<span class="name">' + d.name + '</span><br /><span class="details">' + Number(d.alcohol).toFixed(2) + ' ‰<br />' + d.drinks + " drinks <br /> idle: " + String(Math.floor(d.idletime / 1000 / 60)) + " min</span>";
            }

            var persons;

            function foo(data) {
                $('#drinkers').html('');
                persons = parse_data(data);

                var layout = determine_layout(persons.length);
                layout = pivot_layout_if_necessary(layout);
                for (var i = 0; i < layout[1]; i++) {
                    $('#drinkers').append('<tr id="foo' + i + '"></tr>');
                    for (var j = 0; j < layout[0]; j++) {
                        var colorIndex = i*layout[0] + j;
                        if (colorIndex >= persons.length) continue;
                        var newElement = $('<td>');
                        var person = persons[colorIndex];

                        newElement.attr('id', person.id);
                        newElement.html(getPersonHtml(person));
                        newElement.css('background-color', vari());
                        newElement.click(function() {
                            bar(this);
                        });
                        $('#foo' + i).append(newElement);
                    }
                }

                fix_the_fucking_css();

                initialize(pixinited);
            }

            var inProgress = [];

            function bar(ender) {
                if (inProgress[ender])
                    return;
                inProgress[ender] = true;

                $.get(addDrinkUrl + ender.id, function() { get_data(update); $(ender).fadeTo('slow', 1.0); inProgress[ender] = false; $(ender).css('border-style',
                    'outset'); });
                $(ender).css('border-style', 'inset');
                $(ender).fadeTo('slow', 0.5);
                playSound();
            }

            function fix_the_fucking_css() {
                $("#drinkers").height($(window).height() - $("#topic").height() - 20);
            }

            function pixinited() {
                for (var i in persons) {
                    var person = persons[i];
                    var w = $('#' + person.id).width();
                    var h = $('#' + person.id).height();
                    getChart(person.name, w, h, pixloaded);
                }
            }

            function pixloaded(url, name) {
                var id = "";
                for (var i in persons) {
                    var person = persons[i];
                    if (person.name == name) {
                        id = person.id;
                        break;
                    }
                }
                $('#' + id).css('background-image', 'url(\'' + url + '\')');
            }

            function parse_data(data) {
                var persons = [];
                $(data).find('participant').each(function() {
                    var part = $(this);
                    var person = {};
                    person.name = part.find('name').text();
                    person.alcohol = part.find('alcoholInPromilles').text();
                    person.drinks = part.find('drinks').find('count').text();
                    person.id = part.find('id').text();

                    var newest = new Date(1970, 1, 1)
                    part.find('drinks').find('drink').each(function() {
                        var d = $(this);
                        var timestamp = d.find('timestamp').text();
                        var date = new Date(timestamp);
                        if (date > newest) {
                            newest = date;
                        }
                    });
                    person.idletime = new Date().getTime() - newest.getTime();

                    persons.push(person);
                });
                return persons;
            }

            function playSound() {
                var filename = "/static/sounds/" + Math.floor(Math.random() * 8 + 1) + ".wav.ogg";
                var snd = new Audio(filename);
                snd.play();
            }

            function openAddDrinkerDialog() {
                $('#addDrinkerDialog').css('display', 'block')
                                        .css('left', '50px')
                                        .css('top', '50px');
            }

            function closeAddDrinkerDialog() {
                $('#addDrinkerDialog').css('display', 'none');
            }

            function checkDrinkerName() {
                var drinkerName = $('#drinkerName').val();
                for (var i in persons) {
                    if ( persons[i].name == drinkerName ) {
                        $('#drinkerName').css('background-color', 'red');
                        alert(drinkerName + ' on jo käytössä! Valitse toinen nimi.')
                        $('#drinkerName').focus();
                        return;
                    }
                }

                $('#drinkerName').css('background-color', 'white');
            }
        </script>
    </head>

    <body>
        <div id="header">
            <h1 id="topic"><c:out value="${party.id}" /> <a href="#" onClick="openAddDrinkerDialog();">+</a></h1>
        </div>

        <table id="drinkers">
        </table>

        <div id="addDrinkerDialog" style="display: none; position: absolute;">
            <span style="float: right;"><a href="#" onClick="closeAddDrinkerDialog();">X</a></span>
            <form method="post" action="<c:url value="addParticipant" />">
                  <input type="hidden" name="partyId" value="${party.id}" />
                <table>
                    <tr>
                        <td>Nimi</td>
                        <td><input id="drinkerName" type="text" name="name" onBlur="checkDrinkerName();" /></td>
                    </tr>
                    <tr>
                        <td>Sukupuoli</td>
                        <td>
                            <select name="sex">
                                <option value="MALE">Mies</option>
                                <option value="FEMALE">Nainen</option>
                            </select>
                        </td>
                    </tr>
                    <tr>
                        <td>Paino</td>
                        <td><input type="text" name="weight" /></td>
                    </tr>
                </table>
                <input type="submit" value="Lisää bilettäjä" onClick="forceRefresh();" />
            </form>
        </div>
    </body>
</html>

