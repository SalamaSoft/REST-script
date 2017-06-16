$json = new (function () {
    var _innerType = Java.type('com.alibaba.fastjson.JSON');

    this.serviceName = function() {
        return "$json";
    };

    this.parse = function(str) {
        return _innerType.parse(str);
    };

    this.stringfy = function(obj) {
        return _innerType.toJSONString(obj);
    };

});
