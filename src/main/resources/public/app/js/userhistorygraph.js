var drinksUrl = '/API/v2/profile/drink-history';

function UserHistoryGraph(user, container) {
    var that = this;
    var DAY = 24 * 60 * 60 * 1000;
    this.user = user;
    this.chart = null;
    this.canvas = container.querySelector('canvas');

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

    function createChart(points, min, max) {
        var reducedMotion = window.matchMedia('(prefers-reduced-motion: reduce)').matches;

        that.chart = new Chart(that.canvas, {
            type: 'bar',
            data: {
                datasets: [{
                    data: points,
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
                        min: min,
                        max: max,
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

    this.gotData = function (data) {
        var rows = data.split('\n');
        var points = [];
        var timezoneoffset = -1 * 1000 * 60 * new Date().getTimezoneOffset();

        var drinksByDay = {};
        var first = null, last = null;

        for (var i = 1; i < rows.length; i++) {
            if (rows[i].length === 0) continue;
            var columns = rows[i].split(',');
            var day = Number(columns[0]) + timezoneoffset;
            drinksByDay[day] = Number(columns[1]);
            first = first === null ? day : Math.min(first, day);
            last = last === null ? day : Math.max(last, day);
        }

        if (first === null) return;

        if (last - first < 30 * DAY) {
            first -= 30 * DAY;
        }

        // A point for every day, drinks or not, keeps each bar one day wide.
        for (var x = first; x <= last; x += DAY) {
            points.push({x: x, y: drinksByDay.hasOwnProperty(x) ? drinksByDay[x] : null});
        }

        var min = first - DAY / 2;
        var max = last + DAY / 2;

        if (that.chart) {
            that.chart.options.scales.x.min = min;
            that.chart.options.scales.x.max = max;
            that.chart.data.datasets[0].data = points;
            that.chart.update();
        } else {
            createChart(points, min, max);
        }
    };

    // The server embeds this user's own drink-history CSV into the page
    // on load (see templates/app/index.html) so the very first update() can skip the
    // request. Once used it's cleared so later refreshes always hit the API.
    this.update = function () {
        if (window.__INITIAL_DRINK_HISTORY__) {
            var initialDrinkHistory = window.__INITIAL_DRINK_HISTORY__;
            window.__INITIAL_DRINK_HISTORY__ = null;
            that.gotData(initialDrinkHistory);
            return;
        }

        fetch(drinksUrl, {credentials: 'same-origin'})
            .then(function (response) { return response.text(); })
            .then(that.gotData);
    };

    this.destroy = function () {
        if (that.chart) {
            that.chart.destroy();
            that.chart = null;
        }
    };
}
