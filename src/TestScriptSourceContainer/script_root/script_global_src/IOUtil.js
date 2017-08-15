IOUtil = new (function () {
    var StringBuilder = Java.type("java.lang.StringBuilder");
    var CharArray = Java.type('char[]');

    this.serviceName = function() {
        return "IOUtil";
    };

    this.readReaderAsString = function(reader) {
        var str = new StringBuilder();
        var cBuf = new CharArray(256);

        var bufLen = cBuf.length;
        var readLen;
        while(true) {
            readLen = reader.read(cBuf, 0, bufLen);

            if(readLen < 0) {
                break;
            }

            if(readLen > 0) {
                str.append(cBuf, 0, readLen);
            }
        }

        return str.toString();
    };

});
