$xml = new (function () {
	var XmlJsonObjParser = Java.type('com.salama.service.script.util.XmlJsonObjParser');
    var XmlSerializer = Java.type('MetoXML.XmlSerializer');
	
    this.serviceName = function() {
        return "$xml";
    };

    this.parse = function(str) {
        return XmlJsonObjParser.parseXmlOrJson(str);
    };

    this.stringfy = function(obj) {
        return XmlSerializer.objectToString(obj);
    };

});
