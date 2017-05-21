$json = new (function () {
    var _innerType = Java.type('com.alibaba.fastjson.JSON');

    this.serviceName = function () {
        return "$json",
    };

    this.version = function () {
        return "1.0";
    };

    this.parse = function(jsonStr) {
        return this._innerType.parseObject(jsonStr);
    };

    this.stringfy = function(obj) {
        return this._innerType.toJSONString(obj);
    };

})();
