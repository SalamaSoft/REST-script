package com.salama.service.script.util;

import java.io.IOException;
import java.io.Reader;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.alibaba.fastjson.JSON;

import MetoXML.XmlDeserializer;
import MetoXML.Base.XmlParseException;

public class XmlJsonObjParser {
    public static class ContentsType {
        public final static int Json = 0;
        public final static int XmlOfList = 1;
        public final static int XmlOfObject = 2;
    }

    /**
     * 
     * @param contents XML or JSON.
     * @return parsed Map
     * @throws XmlParseException 
     * @throws IOException 
     * @throws NoSuchMethodException 
     * @throws InstantiationException 
     * @throws IllegalAccessException 
     * @throws InvocationTargetException 
     */
    @SuppressWarnings("unchecked")
    public static Map<String, Object> parseXmlOrJsonObj(String contents) throws InvocationTargetException, IllegalAccessException, InstantiationException, NoSuchMethodException, IOException, XmlParseException {
        if(isXml(contents)) {
            return (Map<String, Object>) XmlDeserializer.stringToObject(contents, Map.class);
        } else {
            //json
            return JSON.parseObject(contents);
        }
    }
    
    public static List<?> parseXmlOrJsonArray(String contents) throws InvocationTargetException, IllegalAccessException, InstantiationException, NoSuchMethodException, IOException, XmlParseException {
        if(isXml(contents)) {
            return (List<?>) XmlDeserializer.stringToObject(contents, ArrayList.class);
        } else {
            //json
            return JSON.parseArray(contents);
        }
    }
    
    public static Object parseXmlOrJson(String contents) throws InvocationTargetException, IllegalAccessException, InstantiationException, NoSuchMethodException, IOException, XmlParseException {
        int contentsType = detectContentsType(contents);
        
        if(contentsType == ContentsType.Json) {
            return JSON.parse(contents);
        } else if (contentsType == ContentsType.XmlOfList) {
            return XmlDeserializer.stringToObject(contents, ArrayList.class);
        } else {
            return XmlDeserializer.stringToObject(contents, HashMap.class);
        }
    }
    
    public static boolean isXml(String contents) {
        int len = contents.length();
        
        for(int i = 0; i < len; i++) {
            char chr = contents.charAt(i);
            
            if(chr <= ' ' || chr >= 127) {
                //not display char
            } else {
                if(chr == '<') {
                    return true;
                } else {
                    return false;
                }
            }
        }
        
        return true;
    }
    
    public static int detectContentsType(String contents) {
        //"<List>".length = 6
        int max = contents.length() - 6;
        int i = 0;
        boolean isXml = false;
        while (i <= max) {
            char chr = contents.charAt(i);
            
            if(chr <= ' ' || chr >= 127) {
                //not display char
            } else {
                if(chr == '<') {
                    isXml = true;
                    
                    if(contents.charAt(i + 1) == '?') {
                        //skip until '>'
                        
                        i++;
                        while(i <= max) {
                            if(contents.charAt(i) == '>') {
                                break;
                            }
                            
                            i++;
                        }
                    } else {
                        if(contents.charAt(i + 1) == 'L'
                                && contents.charAt(i + 2) == 'i'
                                && contents.charAt(i + 3) == 's'
                                && contents.charAt(i + 4) == 't'
                                && contents.charAt(i + 5) == '>'
                                ) {
                            return ContentsType.XmlOfList;
                        } else {
                            return ContentsType.XmlOfObject;
                        }
                    }
                } else {
                    if(isXml) {
                        return ContentsType.XmlOfObject;
                    } else {
                        return ContentsType.Json;
                    }
                }
            }
            i++;
        }
        
        return ContentsType.XmlOfObject;
    }
    
    public static String readAsString(Reader reader) throws IOException {
        StringBuilder str = new StringBuilder();
        char[] cBuf = new char[512];

        int bufLen = cBuf.length;
        int readLen;
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
    }
    
}
