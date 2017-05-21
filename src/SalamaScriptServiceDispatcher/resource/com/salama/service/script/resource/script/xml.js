$xml = new (function () {
    var HashMap = Java.type('java.util.HashMap');
    var _innerTypeDes = Java.type('MetoXML.XmlDeserializer');
    var _innerTypeSer = Java.type('MetoXML.XmlSerializer');
    var _innerTypeMap = (new HashMap()).getClass();

    this.serviceName = function () {
        return "$xml",
    };

    this.version = function () {
        return "1.0";
    };

    this.parse = function(jsonStr) {
        return _innerTypeDes.stringToObject(jsonStr, _innerTypeMap);
    };

    this.stringfy = function(obj) {
        return _innerTypeSer.objectToString(obj);
    };

})();
