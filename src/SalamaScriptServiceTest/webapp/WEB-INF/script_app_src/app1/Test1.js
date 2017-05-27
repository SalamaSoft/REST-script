Test1 = new (function () {
    var logger = LoggerUtil.getLogger($getApp() + "/" + "Test1" + ".js");

    this.serviceName = function() {
        return "Test1";
    };

    this.test = function(params) {
        logger.debug("test() ------ params:" + params);

        var result = {};

        for(var name in params) {
            logger.debug("params[" + name + "] -> " + params[name]);

            result[name] = params[name];
        }

        return result;
    };

});
