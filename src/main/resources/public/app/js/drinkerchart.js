(function (angular) {
    'use strict';

    function DrinkerChart(canvas, participant) {
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
                        afterBuildTicks: wholeHourTicks,
                        ticks: {callback: formatHour}
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
