TestInternalService = new (function () {
    var logger = LoggerUtil.getLogger($getApp() + "/" + "TestInternalService" + ".js");

    this.run = function(params) {
        logger.debug("TestInternalService -----------------");
    };

});
