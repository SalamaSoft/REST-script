$json = new (function () {
    var _innerType = Java.type('com.alibaba.fastjson.JSON');

    this.serviceName = function() {
        return "$json";
    };

    this.parse = function(str) {
        return this._innerType.parseObject(str);
    };

    this.stringfy = function(obj) {
        return this._innerType.toJSONString(obj);
    };

});
