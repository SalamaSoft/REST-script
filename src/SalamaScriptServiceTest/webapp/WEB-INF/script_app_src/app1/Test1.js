Test1 = new (function () {
    var logger = LoggerUtil.getLogger("Test1");

    this.serviceName = function() {
        return "Test1";
    };

    this.test = function(params) {
        logger.debug("test() ------");

        var result = {};

        for(var name in params) {
            logger.debug("params[" + name + "] -> " + params[name]);

            result[name] = params[name];
        }

        return result;
    };

});
