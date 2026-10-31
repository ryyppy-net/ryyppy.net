(function (angular) {
    'use strict';

    function DrinkerChart(canvas, participant, party) {
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

        function wholeHourTicks(scale) {
            var hour = 60 * 60 * 1000;
            var ticks = [];
            for (var t = Math.ceil(scale.min / hour) * hour; t <= scale.max; t += hour) {
                ticks.push({value: t});
            }
            scale.ticks = ticks;
        }

        function partyWindow(party) {
            if (!party || !party.startTime || !party.endTime) {
                return null;
            }
            var timezoneoffset = -1 * 1000 * 60 * new Date().getTimezoneOffset();
            return {
                start: new Date(party.startTime).getTime() + timezoneoffset,
                end: new Date(party.endTime).getTime() + timezoneoffset
            };
        }

        var shownWindow = partyWindow(party);

        var partyWindowPlugin = {
            id: 'partyWindow',
            beforeDatasetsDraw: function (chart) {
                if (!shownWindow) {
                    return;
                }
                var area = chart.chartArea;
                var x = chart.scales.x;
                if (shownWindow.end < x.min || shownWindow.start > x.max) {
                    return;
                }
                var left = Math.max(x.getPixelForValue(shownWindow.start), area.left);
                var right = Math.min(x.getPixelForValue(shownWindow.end), area.right);
                var ctx = chart.ctx;
                ctx.save();
                ctx.fillStyle = 'rgba(0, 0, 0, 0.10)';
                ctx.fillRect(left, area.top, right - left, area.bottom - area.top);
                ctx.strokeStyle = 'rgb(0, 0, 0)';
                ctx.lineWidth = 1;
                if (shownWindow.start >= x.min && shownWindow.start <= x.max) {
                    ctx.beginPath();
                    ctx.moveTo(left, area.top);
                    ctx.lineTo(left, area.bottom);
                    ctx.stroke();
                }
                if (shownWindow.end >= x.min && shownWindow.end <= x.max) {
                    ctx.setLineDash([4, 3]);
                    ctx.beginPath();
                    ctx.moveTo(right, area.top);
                    ctx.lineTo(right, area.bottom);
                    ctx.stroke();
                }
                ctx.restore();
            }
        };

        var chart = new Chart(canvas, {
            plugins: [partyWindowPlugin],
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
                        afterBuildTicks: wholeHourTicks,
                        ticks: {callback: formatHour}
                    },
                    y: {min: 0}
                }
            }
        });

        this.update = function (participant, party) {
            shownWindow = partyWindow(party);
            chart.data.datasets[0].data = historyPoints(participant);
            chart.update();
        };

        this.destroy = function () {
            chart.destroy();
        };
    }

    angular.module('ryyppy').value('DrinkerChart', DrinkerChart);
})(angular);
