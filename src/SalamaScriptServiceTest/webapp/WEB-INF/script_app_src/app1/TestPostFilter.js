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

    this.doPostFilter = function (target, result, request, response) {
    	logger.debug("doPostFilter() -" 
    			+ " target:" + target.app + "/" + target.service + "." + target.method
    			+ " result:" + $json.stringify(result)
    			+ " request:" + request
    			+ " response:" + response
    			);
    	
    	result.filterval = "postfilter test";
    	return result;
    };
            
});
