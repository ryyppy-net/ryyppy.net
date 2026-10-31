(function (angular) {
    'use strict';

    var MINUTE = 60 * 1000;
    var HOUR = 60 * MINUTE;
    var DAY = 24 * HOUR;
    var TICK_STEPS = [15 * MINUTE, 30 * MINUTE, HOUR, 2 * HOUR, 3 * HOUR, 6 * HOUR, 12 * HOUR,
        DAY, 2 * DAY, 7 * DAY, 14 * DAY, 28 * DAY];
    // A tile's chart is about 240px wide on a phone.
    var MAX_TICKS = 6;
    var WEEKDAYS = ['su', 'ma', 'ti', 'ke', 'to', 'pe', 'la'];

    function pad(n) {
        return ('0' + n).slice(-2);
    }

    /**
     * A drinker's promille curve. With partyHistory the history is the party's own, from its
     * start up to now or its end, and the ticks thin out as the party grows longer.
     */
    function DrinkerChart(canvas, participant, partyHistory) {
        var reducedMotion = window.matchMedia('(prefers-reduced-motion: reduce)').matches;

        function historyPoints(participant) {
            var points = [];
            var timezoneoffset = -1 * 1000 * 60 * new Date().getTimezoneOffset();
            var rows = (participant && participant.history) || [];

            for (var i = 0; i < rows.length; i++) {
                var row = rows[i];
                if (row.length === 0) {
                    continue;
                }
                points.push({x: row.timestamp + timezoneoffset, y: row.promilles, projected: row.projected === true});
            }
            return points;
        }

        function formatHour(timestamp) {
            return ('0' + new Date(timestamp).getUTCHours()).slice(-2);
        }

        function ticksEvery(scale, step) {
            var ticks = [];
            for (var t = Math.ceil(scale.min / step) * step; t <= scale.max; t += step) {
                ticks.push({value: t});
            }
            scale.ticks = ticks;
        }

        function wholeHourTicks(scale) {
            ticksEvery(scale, HOUR);
        }

        var partyStep = HOUR;

        function partyTicks(scale) {
            partyStep = TICK_STEPS[TICK_STEPS.length - 1];
            for (var i = 0; i < TICK_STEPS.length; i++) {
                if ((scale.max - scale.min) / TICK_STEPS[i] <= MAX_TICKS) {
                    partyStep = TICK_STEPS[i];
                    break;
                }
            }
            ticksEvery(scale, partyStep);
        }

        // Timestamps are shifted to local time, so the UTC fields read as local ones.
        function formatPartyTick(timestamp) {
            var date = new Date(timestamp);
            if (partyStep >= DAY) {
                return date.getUTCDate() + '.' + (date.getUTCMonth() + 1) + '.';
            }
            if (partyStep < HOUR) {
                return pad(date.getUTCHours()) + ':' + pad(date.getUTCMinutes());
            }
            return timestamp % DAY === 0 ? WEEKDAYS[date.getUTCDay()] : formatHour(timestamp);
        }

        var chart = new Chart(canvas, {
            type: 'line',
            data: {
                datasets: [{
                    data: historyPoints(participant),
                    borderColor: 'rgb(0, 0, 0)',
                    borderWidth: 2,
                    pointRadius: 0,
                    pointHitRadius: 0,
                    segment: {
                        borderDash: function (ctx) {
                            return ctx.chart.data.datasets[0].data[ctx.p1DataIndex].projected ? [6, 4] : undefined;
                        }
                    }
                }]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                animation: reducedMotion ? false : {},
                events: [],
                plugins: {legend: {display: false}, tooltip: {enabled: false}},
                scales: {
                    x: {
                        type: 'linear',
                        bounds: 'data',
                        afterBuildTicks: partyHistory ? partyTicks : wholeHourTicks,
                        ticks: {callback: partyHistory ? formatPartyTick : formatHour}
                    },
                    y: {min: 0}
                }
            }
        });

        this.update = function (participant) {
            chart.data.datasets[0].data = historyPoints(participant);
            chart.update();
        };

        this.destroy = function () {
            chart.destroy();
        };
    }

    angular.module('ryyppy').value('DrinkerChart', DrinkerChart);
})(angular);
