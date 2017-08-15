LoggerUtil = new (function () {
    var RuntimeException = Java.type("java.lang.RuntimeException");

    this.serviceName = function() {
        return "LoggerUtil";
    };

    var LogFactory = Java.type("org.apache.commons.logging.LogFactory");
    this.getLogger = function(loggerName) {
        return LogFactory.getLog(loggerName);
    };
    
    this.wrapError = function (e) {
        if(e.class) {
            return e;
        } else {
            return new RuntimeException(String(e.stack));
        }
    }
});
