$json = {
  _innerType: Java.type('com.alibaba.fastjson.JSON'),
  parse: function(jsonStr) {
    return this._innerType.parseObject(jsonStr);
  },
  stringfy: function(obj) {
    return this._innerType.toJSONString(obj);
  },
};
