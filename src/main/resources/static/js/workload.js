var backgroundColors = [
    'rgba(255, 99, 132, 0.2)',
    'rgba(255, 159, 64, 0.2)',
    'rgba(255, 205, 86, 0.2)',
    'rgba(75, 192, 192, 0.2)',
    'rgba(54, 162, 235, 0.2)',
    'rgba(153, 102, 255, 0.2)',
    'rgba(201, 203, 207, 0.2)'
];

var borderColors = [
    'rgb(255, 99, 132)',
    'rgb(255, 159, 64)',
    'rgb(255, 205, 86)',
    'rgb(75, 192, 192)',
    'rgb(54, 162, 235)',
    'rgb(153, 102, 255)',
    'rgb(201, 203, 207)'
];

$('document').ready(function () {
    var tooltipTriggerList = [].slice.call(document.querySelectorAll('[data-bs-toggle="tooltip"]'))
    tooltipTriggerList.map(function (tooltipTriggerEl) {
        return new bootstrap.Tooltip(tooltipTriggerEl)
    })

    CodeMirror.fromTextArea(document.getElementById('datasource-config-editor'), {
        lineNumbers: true,
        lineWrapping: true,
        readOnly: false,
        mode: 'text/x-yaml',
        theme: 'abbott'
    });
});

const WorkloadDashboard = function (settings) {
    this.settings = settings;
    this.init();
};

WorkloadDashboard.prototype = {
    init: function () {
        var socket = new SockJS(this.settings.endpoints.socket),
                stompClient = Stomp.over(socket),
                _this = this;
        // stompClient.log = (log) => {};
        stompClient.connect({}, function (frame) {
            stompClient.subscribe(_this.settings.topics.refresh, function () {
                location.reload();
            });

            stompClient.subscribe(_this.settings.topics.update, function () {
                _this.handleModelUpdate();
            });

            stompClient.subscribe(_this.settings.topics.charts, function () {
                _this.handleChartsUpdate();
            });
        });
    },

    getElement: function (id) {
        return $('#' + id);
    },

    round: function (v) {
        return v.toFixed(1);
    },

    handleModelUpdate: function () {
        var _this = this;

        const queryString = window.location.search;

        $.getJSON("workload/items" + queryString, function(json) {
            json.map(function (workload) {
                _this.handleWorkloadItemsUpdate(workload);
            });
        });

        $.getJSON("workload/summary" + queryString, function(json) {
            _this.handleWorkloadSummaryUpdate(json);
        });
    },

    handleWorkloadItemsUpdate: function (workload) {
        var _this = this;

        const rowElt = _this.getElement("row-" +  workload.id);
        rowElt.find(".remaining-time").text(workload.remainingTime);
        rowElt.find(".p90").text(_this.round(workload.metrics.p90));
        rowElt.find(".p99").text(_this.round(workload.metrics.p99));
        rowElt.find(".p999").text(_this.round(workload.metrics.p999));
        rowElt.find(".opsPerSec").text(_this.round(workload.metrics.opsPerSec));
        rowElt.find(".opsPerMin").text(_this.round(workload.metrics.opsPerMin));
        rowElt.find(".success").text(workload.metrics.success);
        rowElt.find(".transientFail").text(workload.metrics.transientFail);
        rowElt.find(".nonTransientFail").text(workload.metrics.nonTransientFail);
        rowElt.find(".status").attr("class", "badge status " + workload.statusBadge);
        rowElt.find(".status").text(workload.status);
    },

    handleWorkloadSummaryUpdate: function (metrics) {
        var _this = this;

        const metricElt = _this.getElement("aggregated-metrics");
        metricElt.find(".p90").text(_this.round(metrics.p90));
        metricElt.find(".p99").text(_this.round(metrics.p99));
        metricElt.find(".p999").text(_this.round(metrics.p999));
        metricElt.find(".opsPerSec").text(_this.round(metrics.opsPerSec));
        metricElt.find(".opsPerMin").text(_this.round(metrics.opsPerMin));
        metricElt.find(".success").text(metrics.success);
        metricElt.find(".transientFail").text(metrics.transientFail);
        metricElt.find(".nonTransientFail").text(metrics.nonTransientFail);
    },
};

document.addEventListener('DOMContentLoaded', function () {
    new WorkloadDashboard({
        endpoints: {
            socket: '/pooltool-service',
        },

        topics: {
            update: '/topic/workload/update',
            refresh: '/topic/workload/refresh',
        },
    });
});

