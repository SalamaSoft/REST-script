LoggerUtil = new (function () {
    var Logger = Java.type('org.apache.log4j.Logger');

    this.serviceName = function() {
        return "LoggerUtil";
    };

    this.getLogger = function(loggerName) {
        return Logger.getLogger(loggerName);
    };

});
