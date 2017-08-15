Test1 = new (function () {
    var logger = LoggerUtil.getLogger($getApp() + "/" + "Test1" + ".js");

    this.serviceName = function() {
        return "Test1";
    };
    

    this.test = function(p0, p1, p2) {
        logger.debug("test() p0:" + p0);
        logger.debug("test() p1:" + p1);
        logger.debug("test() p2:" + p2);

        var args = arguments;
        for(var i = 0; i < args.length; i++) {
            logger.debug("args[" + i + "] -> " + args[i]);
        }
        
        TestInternalService.run();
        
        return ">>>>>>>> Test1.test";
    };

});
