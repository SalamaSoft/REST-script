Test1 = new (function () {
    var logger = LoggerUtil.getLogger($getApp() + "/" + "Test2" + ".js");

    this.serviceName = function() {
        return "Test2";
    };

    this.test = function(params) {
        logger.debug("test2() ------ params:" + params);

        var result = {};

        for(var name in params) {
            logger.debug("params[" + name + "] -> " + params[name]);

            result[name] = params[name];
        }

        TestInternalService.run();
        
        return result;
    };

});
