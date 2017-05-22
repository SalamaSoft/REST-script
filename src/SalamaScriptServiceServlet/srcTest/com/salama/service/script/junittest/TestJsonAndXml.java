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
import org.omg.CORBA.portable.InvokeHandler;

import com.alibaba.fastjson.JSON;
import com.salama.service.script.core.IScriptContext;

import MetoXML.XmlDeserializer;
import MetoXML.XmlSerializer;
import MetoXML.Base.XmlParseException;
import MetoXML.Util.ClassFinder;

public class TestJsonAndXml {

    private final static String SCRIPT_JSONParser = ""
            + "  $json = {\n"
            + "    _innerType: Java.type('com.alibaba.fastjson.JSON'),\n"
            + "    parse: function(jsonStr) {\n"
            + "      return this._innerType.parseObject(jsonStr);\n"
            + "    },\n"
            + "    stringfy: function(obj) {\n"
            + "      return this._innerType.toJSONString(obj);\n"
            + "    },\n"
            + "  };\n"
            ;
        
    private final static String SCRIPT_XmlParser = ""
            + "  var HashMap = Java.type('java.util.HashMap');\n"
            + "  $xml = {\n"
            + "    _innerTypeDes: Java.type('MetoXML.XmlDeserializer'),\n"
            + "    _innerTypeSer: Java.type('MetoXML.XmlSerializer'),\n"
            + "    _innerTypeMap: (new HashMap()).getClass(),\n"
            + "    parse: function(jsonStr) {\n"
            + "      return this._innerTypeDes.stringToObject(jsonStr, this._innerTypeMap);\n"
            + "    },\n"
            + "    stringfy: function(obj) {\n"
            + "      return this._innerTypeSer.objectToString(obj);\n"
            + "    },\n"
            + "    test: function() {\n"
            + "      var old = this.flag;\n"
            + "      this.flag = !this.flag;\n"
            + "      print('test flag changed:' + old + ' --> ' + this.flag);"
            + "    },"
            + "    flag: false,"
            + "  };\n"
            ;
    
    @Test
    public void testInterface() {
        try {
            final ScriptEngineManager engineManager = new ScriptEngineManager();
            {
                final ScriptEngine engine = engineManager.getEngineByName("nashorn");
                final Invocable jsInvoke = (Invocable) engine;
                final Compilable jsCompiler = (Compilable) engine;
                
                String script = ""
                        + "Test1 = new ("
                        + "function() {\n"
                        + "  this.reload = function(a, b, c) {\n"
                        + "      print("
                        + "        'reload() arguments.length:' + arguments.length"
                        + "      );\n"
                        + "  };\n"
                        + "  this.destroy = function(a, b) {\n"
                        + "  };\n"
                        + "}"
                        + ");"
                        ;
                
                CompiledScript compiledScript = jsCompiler.compile(script);
                IScriptContext inst = jsInvoke.getInterface(compiledScript.eval(), IScriptContext.class);
                System.out.println("getInterface() -> " + inst);
            }
                      
        } catch (Throwable e) {
            e.printStackTrace();
        }
     }
    
    public void testXml_2() {
        try {
            final ScriptEngineManager engineManager = new ScriptEngineManager();
            {
                final ScriptEngine engine = engineManager.getEngineByName("nashorn");
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
                        + "  this.testXml = function(data) {\n"
                        + "      var str = $xml.stringfy(data);\n"
                        + "      print('testXml str  -> ' + str);"
                        + "      var data2 = $xml.parse(str);\n"
                        + "      var str2 = $xml.stringfy(data2);\n"
                        + "      print('testXml str2 -> ' + str2);"
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
                
                Object jsXmlParser = engine.eval(SCRIPT_XmlParser);
                System.out.println(""
                        + " type:" + jsXmlParser.getClass().getName()
                        + " [test] exists:" + ((Map<String, Object>)jsXmlParser).containsKey("test")
                        );
                engineManager.put("$xml", jsXmlParser);

                CompiledScript compiledScript = jsCompiler.compile(script);
                Object jsObj = compiledScript.eval();
                
                Map<String, Object> jsData = (Map<String, Object>) jsInvoke.invokeMethod(jsObj, "makeData");
                testXml(jsData, Map.class);
                
                
                System.out.println("testXml 1 -----------------");
                jsInvoke.invokeMethod(jsObj, "testXml", jsData);
                System.out.println("testXml 2 -----------------");
                jsInvoke.invokeMethod(jsObj, "testXml", makeTestData3());
                
                //test global variable
                engine.eval("$xml.test();");
            }
                      
            //test global variable
            {
                final ScriptEngine engine = engineManager.getEngineByName("nashorn");
                engine.eval("$xml.test();");
            }
            
            {
                final ScriptEngine engine = engineManager.getEngineByName("nashorn");
                engine.eval("$xml.test();");
            }
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }
    
    //@Test
    public void testXml_1() {
        try {
            TestData3 data = makeTestData3();
            testXml(data);
            testXml(data, data.getClass());
            testXml(data, data.getClass(), new ClassFinder() {
                
                @Override
                public Class<?> findClass(String className) throws ClassNotFoundException {
                    if(className.equals(TestData2.class.getSimpleName())) {
                        return TestData2.class;
                    } if(className.equals(TestData1.class.getSimpleName())) {
                        return TestData1.class;
                    } else {
                        return null;
                    }
                }
            });
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
    
    public static void testXml(Object obj) throws IllegalAccessException, InvocationTargetException, IntrospectionException, IOException, InstantiationException, NoSuchMethodException, XmlParseException {
        testXml(obj, obj.getClass(), null);
    }
    
    public static void testXml(Object obj, Class<?> cls) throws IllegalAccessException, InvocationTargetException, IntrospectionException, IOException, InstantiationException, NoSuchMethodException, XmlParseException {
        testXml(obj, cls, null);
    }
    
    public static void testXml(Object obj, Class<?> cls, ClassFinder classFinder) throws IllegalAccessException, InvocationTargetException, IntrospectionException, IOException, InstantiationException, NoSuchMethodException, XmlParseException {
        String xmlStr = XmlSerializer.objectToString(obj, cls);
        System.out.println("xml.tostr -> " + xmlStr);

        Object obj2 = XmlDeserializer.stringToObject(xmlStr, cls, classFinder);
        System.out.println("xml.parse -> " + obj2);
        
        String xmlStr2 = XmlSerializer.objectToString(obj2, cls);
        System.out.println("xml.tostr -> " + xmlStr2);
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
