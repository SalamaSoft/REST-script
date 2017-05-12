package com.salama.service.script.junittest;

import java.beans.IntrospectionException;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;

import javax.script.Compilable;
import javax.script.CompiledScript;
import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

import org.junit.Test;

import com.alibaba.fastjson.JSON;

import MetoXML.XmlSerializer;

public class TestJsonAndXml {

    private final static String SCRIPT_JSONParser = ""
            + "  print('init $json ---');"
            + "  $json = {\n"
            + "    _inner: Java.type('com.alibaba.fastjson.JSON'),"
            + "    parse: function(jsonStr) {\n"
            + "      return this._inner.parseObject(jsonStr);"
            + "    },\n"
            + "    stringfy: function(obj) {\n"
            + "      return this._inner.toJSONString(obj);"
            + "    },\n"
            + "  };\n"
            ;
    
    
    public void testXml_2() {
        try {
            final ScriptEngine engine = createEngine();
            final Invocable jsInvoke = (Invocable) engine;
            final Compilable jsCompiler = (Compilable) engine;
            
            String script = ""
                    + "TestCompiled1 = new ("
                    + "function() {\n"
                    + "  this.test = function(data) {\n"
                    + "      print("
                    + "        'test() data ->'"
                    + "        + ' k1:' + data.k1 "
                    + "        + ' k2:' + data.k2"
                    + "        + ' data.k3.s1:' + data.k3.s1 "
                    + "        + ' data.k3.s2:' + data.k3.s2 "
                    + "        + ' data.k3.s3:' + data.k3.s3 "
                    + "      );\n"
                    + "  };\n"
                    + "  this.testJson = function(data) {\n"
                    + "      var jsonStr = $json.stringfy(data);\n"
                    + "      var data2 = $json.parse(jsonStr);\n"
                    + "      var jsonStr2 = $json.stringfy(data2);\n"
                    + "      print('jsonStr  -> ' + jsonStr);"
                    + "      print('jsonStr2 -> ' + jsonStr2);"
                    + "  };\n"
                    + "  this.makeData = function() {\n"
                    + "    var data = {"
                    + "      k1: 'abcde', \n"
                    + "      k2: '123', \n"
                    + "      k3: {"
                    + "        s1: 'bbbb',"
                    + "        s2: 333.4,"
                    + "        s3: ['ar0', 'ar1', 'ar2', ]"
                    + "      }, \n"
                    + "    };\n"
                    + "    print('typeof data:' + (typeof data));\n"
                    + "    return data;\n"
                    + "  }\n"
                    + "}"
                    + ");"
                    ;

            //Object json = engine.eval("JSON;");
            engine.eval(SCRIPT_JSONParser);
            
            CompiledScript compiledScript = jsCompiler.compile(script);
            Object jsObj = compiledScript.eval();
            
            Map<String, Object> jsData = (Map<String, Object>) jsInvoke.invokeMethod(jsObj, "makeData");
            testXml(jsData, Map.class);
            
            jsInvoke.invokeMethod(jsObj, "test", jsData);

            jsInvoke.invokeMethod(jsObj, "testJson", jsData);
            jsInvoke.invokeMethod(jsObj, "testJson", makeTestData3());
                        
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }
    
    @Test
    public void testXml_1() {
        try {
            TestData3 data = makeTestData3();
            testXml(data, data.getClass());
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }
    
    //@Test
    public void testJson_4() {
        try {
            final ScriptEngine engine = createEngine();
            final Invocable jsInvoke = (Invocable) engine;
            final Compilable jsCompiler = (Compilable) engine;
            
            String script = ""
                    + "TestCompiled1 = new ("
                    + "function() {\n"
                    + "  this.test = function(data) {\n"
                    + "      print("
                    + "        'test() data ->'"
                    + "        + ' k1:' + data.k1 "
                    + "        + ' k2:' + data.k2"
                    + "        + ' data.k3.s1:' + data.k3.s1 "
                    + "        + ' data.k3.s2:' + data.k3.s2 "
                    + "        + ' data.k3.s3:' + data.k3.s3 "
                    + "      );\n"
                    + "  };\n"
                    + "  this.testJson = function(data) {\n"
                    + "      var jsonStr = $json.stringfy(data);\n"
                    + "      var data2 = $json.parse(jsonStr);\n"
                    + "      var jsonStr2 = $json.stringfy(data2);\n"
                    + "      print('jsonStr  -> ' + jsonStr);"
                    + "      print('jsonStr2 -> ' + jsonStr2);"
                    + "  };\n"
                    + "  this.makeData = function() {\n"
                    + "    var data = {"
                    + "      k1: 'abcde', \n"
                    + "      k2: '123', \n"
                    + "      k3: {"
                    + "        s1: 'bbbb',"
                    + "        s2: 333.4,"
                    + "        s3: ['ar0', 'ar1', 'ar2', ]"
                    + "      }, \n"
                    + "    };\n"
                    + "    print('typeof data:' + (typeof data));\n"
                    + "    return data;\n"
                    + "  }\n"
                    + "}"
                    + ");"
                    ;

            //Object json = engine.eval("JSON;");
            engine.eval(SCRIPT_JSONParser);
            
            CompiledScript compiledScript = jsCompiler.compile(script);
            Object jsObj = compiledScript.eval();
            
            Map<String, Object> jsData = (Map<String, Object>) jsInvoke.invokeMethod(jsObj, "makeData");
            testJson(jsData, Map.class);
            
            jsInvoke.invokeMethod(jsObj, "test", jsData);
            jsInvoke.invokeMethod(jsObj, "testJson", jsData);
            jsInvoke.invokeMethod(jsObj, "testJson", makeTestData3());
                        
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }
    
    public void testJson_3() {
        testJson(makeTestData3());
    }
    
    public void testJson_2() {
        testJson(makeTestData2());
    }
    
    public void testJson_1() {
        testJson(makeComplexMap());
    }
    
    public static void testJson(Object obj) {
        testJson(obj, obj.getClass());
    }
    
    public static void testJson(Object obj, Class<?> cls) {
        String jsonStr = JSON.toJSONString(obj);
        System.out.println("json.tostr -> " + jsonStr);

        Object obj2 = JSON.parseObject(jsonStr, cls);
        System.out.println("json.parse -> " + obj2); 
    }
    
    public static void testXml(Object obj) throws IllegalAccessException, InvocationTargetException, IntrospectionException, IOException {
        testXml(obj, obj.getClass());
    }
    
    public static void testXml(Object obj, Class<?> cls) throws IllegalAccessException, InvocationTargetException, IntrospectionException, IOException {
        String jsonStr = XmlSerializer.objectToString(obj, cls);
        System.out.println("xml.tostr -> " + jsonStr);

//        Object obj2 = JSON.parseObject(jsonStr, cls);
//        System.out.println("xml.parse -> " + obj2); 
    }
    
    
    public static Map<String, Object> makeComplexMap() {
        Map<String, Object> map = makePlainMap();
        map.put("k4", makePlainMap());
        ((Map<String, Object>) map.get("k4")).put("k5", makePlainMap());
        
        return map;
    }
    
    public static Map<String, Object> makePlainMap() {
        Map<String, Object> map = new HashMap<>();
        
        map.put("k1", "v1");
        map.put("k2", "v2");
        map.put("k3", "v3");
        
        return map;
    }

    public static TestData3 makeTestData3() {
        TestData3 data = new TestData3();
        
        data.setK3("kv3");
        data.setData1(makeTestData1());
        
        Map<String, Object> map = new HashMap<>();
        map.put("m1", "mv1");
        map.put("m2", 199.0);
        map.put("d1", makeTestData1());
        map.put("ar1", new String[]{"ar0", "ar1", "ar2"});
        map.put("ar2", new TestData2[]{makeTestData2(), makeTestData2()});
        
        data.setValMap(map);
        
        return data;
    }

    public static TestData2 makeTestData2() {
        TestData2 data2 = new TestData2();
        data2.setK3("v3");
        
        data2.setData1(makeTestData1());
        
        return data2;
    }
    
    public static TestData1 makeTestData1() {
        TestData1 data1 = new TestData1();
        data1.setK1("v1");
        data1.setK2("v2");
        
        return data1;
    }
    
    public static class TestData3 {
        private String _k3;
        
        private TestData1 _data1;
        
        private Map<String, Object> _valMap;

        public String getK3() {
            return _k3;
        }

        public void setK3(String k3) {
            _k3 = k3;
        }

        public TestData1 getData1() {
            return _data1;
        }

        public void setData1(TestData1 data1) {
            _data1 = data1;
        }

        public Map<String, Object> getValMap() {
            return _valMap;
        }

        public void setValMap(Map<String, Object> valMap) {
            _valMap = valMap;
        }
        
        
    } 

    public static class TestData2 {
        private String _k3;
        
        private TestData1 _data1;

        public String getK3() {
            return _k3;
        }

        public void setK3(String k3) {
            _k3 = k3;
        }

        public TestData1 getData1() {
            return _data1;
        }

        public void setData1(TestData1 data1) {
            _data1 = data1;
        }
        
        
    } 
    
    public static class TestData1 {
        private String _k1;
        
        private String _k2;

        public String getK1() {
            return _k1;
        }

        public void setK1(String k1) {
            _k1 = k1;
        }

        public String getK2() {
            return _k2;
        }

        public void setK2(String k2) {
            _k2 = k2;
        }
        
    }
    
    private static ScriptEngine createEngine() {
        final ScriptEngineManager engineManager = new ScriptEngineManager();
        return engineManager.getEngineByName("nashorn");
    }
    
}
