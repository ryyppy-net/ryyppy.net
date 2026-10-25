(function (angular) {
    'use strict';

    var DAY = 24 * 60 * 60 * 1000;

    function formatDate(timestamp) {
        var d = new Date(timestamp);
        return d.getUTCDate() + '.' + (d.getUTCMonth() + 1) + '.' + d.getUTCFullYear();
    }

    function dayTicks(scale) {
        var ticks = [];
        for (var t = scale.min + DAY / 2; t < scale.max; t += DAY) {
            ticks.push({value: t});
        }
        scale.ticks = ticks;
    }

    function utcDay(time) {
        return Math.floor(time / DAY) * DAY;
    }

    // Drinks per UTC day from the first drink to today, at least 30 days. A
    // point for every day, drinks or not, keeps each bar one day wide.
    function perDay(drinks) {
        var counts = {};
        var last = utcDay(Date.now());
        var first = last;

        drinks.forEach(function (drink) {
            var day = utcDay(Date.parse(drink.timestamp));
            counts[day] = (counts[day] || 0) + 1;
            first = Math.min(first, day);
            last = Math.max(last, day);
        });

        if (last - first < 30 * DAY) {
            first -= 30 * DAY;
        }

        var points = [];
        for (var x = first; x <= last; x += DAY) {
            points.push({x: x, y: counts.hasOwnProperty(x) ? counts[x] : null});
        }
        return {points: points, min: first - DAY / 2, max: last + DAY / 2};
    }

    function createChart(canvas) {
        var reducedMotion = window.matchMedia('(prefers-reduced-motion: reduce)').matches;

        return new Chart(canvas, {
            type: 'bar',
            data: {
                datasets: [{
                    data: [],
                    backgroundColor: 'rgb(0, 0, 0)',
                    categoryPercentage: 1,
                    barPercentage: 1
                }]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                animation: reducedMotion ? false : {},
                interaction: {mode: 'index', intersect: false},
                plugins: {
                    legend: {display: false},
                    tooltip: {
                        callbacks: {
                            title: function () { return ''; },
                            label: function (context) {
                                return formatDate(context.parsed.x) + ': ' + Math.round(context.parsed.y) + ' annosta';
                            }
                        }
                    }
                },
                scales: {
                    x: {
                        type: 'linear',
                        offset: false,
                        afterBuildTicks: dayTicks,
                        ticks: {
                            maxRotation: 0,
                            callback: formatDate
                        }
                    },
                    y: {min: 0}
                }
            }
        });
    }

    function DrinkHistoryGraphCtrl($element) {
        var self = this;
        var chart = null;

        function draw() {
            if (!chart || !self.drinks) {
                return;
            }
            var history = perDay(self.drinks);
            chart.options.scales.x.min = history.min;
            chart.options.scales.x.max = history.max;
            chart.data.datasets[0].data = history.points;
            chart.update();
        }

        this.$postLink = function () {
            chart = createChart($element[0].querySelector('canvas'));
            draw();
        };

        this.$onChanges = function (changes) {
            if (changes.drinks) {
                draw();
            }
        };

        this.$onDestroy = function () {
            if (chart) {
                chart.destroy();
                chart = null;
            }
        };
    }

    DrinkHistoryGraphCtrl.$inject = ['$element'];

    angular.module('ryyppy').component('drinkHistoryGraph', {
        controller: DrinkHistoryGraphCtrl,
        bindings: {
            drinks: '<'
        },
        template: `
    <div class="historyGraphContainer">
        <div id="historyGraph">
            <canvas></canvas>
        </div>
    </div>`
    });
})(angular);
