TestContext2 = new (function () {
    var logger = LoggerUtil.getLogger("TestContext2" + ".js");

    this.serviceName = function() {
        return "TestContext2";
    };

    this.reload = function(config, configLocationResolver) {
        logger.debug("reload() ------");

        var configObj =  $xml.parse(IOUtil.readReaderAsString(config));
        logger.debug("configObj to xml:\n" + $xml.stringify(configObj));
        logger.debug("configObj to json:\n" + $json.stringify(configObj));
    };

    this.destroy = function() {
        logger.debug("destroy() ------");
    };

});
