TestPreFilter = new (function () {
    var logger = LoggerUtil.getLogger($getApp() + "/" + "TestPreFilter.js");

    this.serviceName = function() {
        return "TestPreFilter";
    };

    this.reload = function(config, configLocationResolver) {
        logger.debug("reload() ------");
    };

    this.destroy = function() {
        logger.debug("destroy() ------");
    };

    this.doPreFilter = function (target, params, request, response) {
    	logger.debug("doPreFilter() -" 
    			+ " target:" + target.app + "/" + target.service + "." + target.method
    			+ " params:" + $json.stringify(params)
    			+ " request:" + request
    			+ " response:" + response
    			);
    	return {"override": false, "result": "test prefilter"};
    };
            
});
