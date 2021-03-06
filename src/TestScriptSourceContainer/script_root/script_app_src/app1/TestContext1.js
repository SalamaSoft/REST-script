TestContext1 = new (function () {
    var logger = LoggerUtil.getLogger("TestContext1" + ".js");

    this.serviceName = function() {
        return "TestContext1";
    };

    this.reload = function(config, configLocationResolver) {
        logger.debug("reload() ------");

        var configObj = $xml.parse(IOUtil.readReaderAsString(config));
        logger.debug("configObj to xml:\n" + $xml.stringify(configObj));
        logger.debug("configObj to json:\n" + $json.stringify(configObj));
    };

    this.destroy = function() {
        logger.debug("destroy() ------");
    };

});
