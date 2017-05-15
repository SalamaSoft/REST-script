var HashMap = Java.type('java.util.HashMap');
$xml = {
    serviceName: "$xml",
    _innerTypeDes: Java.type('MetoXML.XmlDeserializer'),
    _innerTypeSer: Java.type('MetoXML.XmlSerializer'),
    _innerTypeMap: (new HashMap()).getClass(),
    parse: function(jsonStr) {
        return this._innerTypeDes.stringToObject(jsonStr, this._innerTypeMap);
    },
    stringfy: function(obj) {
        return this._innerTypeSer.objectToString(obj);
    },
};
