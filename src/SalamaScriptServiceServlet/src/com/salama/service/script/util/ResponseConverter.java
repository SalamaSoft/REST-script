package com.salama.service.script.util;

import java.beans.IntrospectionException;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URLEncoder;

import com.alibaba.fastjson.JSON;

import MetoXML.XmlSerializer;

public class ResponseConverter {    
    //public final static String COVERT_TYPE_PLAIN_TEXT = "text";
    public final static String CONVERT_TYPE_XML = "xml";
    public final static String CONVERT_TYPE_JSON = "json";
    //public final static String COVERT_TYPE_PLAIN_TEXT_JSONP = "text.jsonp";
    //public final static String COVERT_TYPE_XML_JSONP = "xml.jsonp";
    //public final static String COVERT_TYPE_JSON_JSONP = "json.jsonp";

    public final static String JSONP_PREFIX = ".jsonp";
    public final static String DEFAULT_JSONP_VAR_NAME = "jsonpReturn";
    
    private final static int LEN_CONVERT_TYPE_XML = CONVERT_TYPE_XML.length();
    private final static int LEN_CONVERT_TYPE_JSON = CONVERT_TYPE_JSON.length();
    private final static int LEN_JSONP_PREFIX = JSONP_PREFIX.length();
    
    /**
     * 
     * @param responseType ('xml'|'json')[.jsonp=$varName] 
     * @param responseVal 
     * @param urlEncoding charset for encodeURL
     * @return "" if responseVal is null.
     * return responseVal directly if typeof responseVal is String,
     * otherwise convert into String with responseType if typeof responseVal is not String.
     * Convert into xml if responseType not start with "json", and convertIn
     * @throws IOException 
     * @throws IntrospectionException 
     * @throws InvocationTargetException 
     * @throws IllegalAccessException 
     */
    public static String convertResponse(
            String responseType,
            Object responseVal,
            String urlEncoding
            ) throws IllegalAccessException, InvocationTargetException, IntrospectionException, IOException {
        if(responseVal == null) {
            return "";
        }
        if(responseVal.getClass() == String.class) {
            return (String) responseVal;
        }
        
        if(responseType == null || responseType.length() == 0) {
            //default xml
            return toXml(responseVal);
        } else {
            //convert to xml or json ------
            int responseTypeLen = responseType.length();
            int convertTypeLen;
            String responseStr;
            if(responseType.startsWith(CONVERT_TYPE_JSON)) {
                responseStr = toJson(responseVal);
                convertTypeLen = LEN_CONVERT_TYPE_JSON;
            } else {
                responseStr = toXml(responseVal);
                convertTypeLen = LEN_CONVERT_TYPE_XML;
            }
            
            String jsonpVarName = null;
            //findout jsonpVarName ------
            if(responseTypeLen > convertTypeLen 
                    && responseType.startsWith(JSONP_PREFIX, convertTypeLen)
                    ) {
                //jsonp 
                int indexEqual = convertTypeLen + LEN_JSONP_PREFIX;
                int indexVarName = indexEqual + 1;
                if(responseTypeLen > indexVarName
                        && responseType.charAt(indexEqual) == '='
                        ) {
                    jsonpVarName = responseType.substring(indexVarName);
                } else {
                    jsonpVarName = DEFAULT_JSONP_VAR_NAME;
                }
            }
            
            if(jsonpVarName == null) {
                return responseStr;
            } else {
                if(urlEncoding == null || urlEncoding.length() == 0) {
                    urlEncoding = "utf-8";
                }
                
                return "var " + jsonpVarName + " = \"" 
                        + URLEncoder.encode(responseStr, urlEncoding).replace("+", "%20") 
                        + "\";\n";
            }
        }
    }
    
    public static String toJson(Object obj) {
        if(obj == null) {
            return "";
        } else {
            return JSON.toJSONString(obj);
        }
    }
    
    public static String toXml(Object obj) throws IllegalAccessException, InvocationTargetException, IntrospectionException, IOException {
        if(obj == null) {
            return "";
        } else {
            return XmlSerializer.objectToString(obj, obj.getClass());
        }
    }
}
