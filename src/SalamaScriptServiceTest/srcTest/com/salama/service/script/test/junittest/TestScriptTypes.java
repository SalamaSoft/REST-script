package com.salama.service.script.test.junittest;

import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Map;
import java.util.function.Predicate;

import javax.script.Bindings;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import org.junit.Test;

public class TestScriptTypes {
    

    public void test_char() {
        try {
            final ScriptEngine engine = createEngine();
            String script = ""
                    + "var CharArray = Java.type('char[]');\n"
                    + "var cbuf = new CharArray(10);\n"
                    + "print('typeof cbuf:' + (typeof cbuf));\n"
                    + "cbuf.length;\n"
                    ;
            Object jsObj = engine.eval(script);
            System.out.println("jsObj:" + jsObj);
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }
    
    public void test_ScriptObj() {
        try {
            final ScriptEngine engine = createEngine();
            String script = ""
                    + "var data = "
                    + "{"
                    + "  k1: 'abcde', \n"
                    + "  k2: '123', \n"
                    + "};\n"
                    + "print('typeof data:' + (typeof data));\n"
                    + "data;"
                    ;
            Object jsObj = engine.eval(script);
            //convert to map
            Map<String, Object> map = (Map<String, Object>) jsObj;
            for (String key : map.keySet()) {
                System.out.println("map[" + key + "]: " + map.get(key));
            } 
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }
    
    public void test_JavaBean() {
        try {
            final ScriptEngine engine = createEngine();
            String script = ""
                    + "var TestData = Java.type('com.salama.service.script.test.junittest.TestData');\n"
                    + "var data = new TestData();"
                    + "data.k1 = 'abcde'; \n"
                    + "data.k2 = '123'; \n"
                    + "print('typeof data:' + (typeof data));\n"
                    + "print('data[k1]:' + data['k1']);\n"
                    ;
            engine.eval(script);
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    @Test
    public void test_tryBlock() {
        try {
            final ScriptEngine engine = createEngine();

            Object retVal = testScriptAndPrint(
                    engine, 
                    "var TestUtil = Java.type('com.salama.service.script.test.junittest.TestUtil'); \n"
                    + "  var test = new TestUtil(); \n"
                    + "  try { \n"
                    + "     print('test.ymdHms():' + test.ymdHms()); \n"
                    + "} finally { \n"
                    + "  test.close(); \n"
                    + "}\n"
                    );
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }
    
    public void test_staticMethod() {
        try {
            final ScriptEngine engine = createEngine();

            Object retVal = testScriptAndPrint(
                    engine, 
                    "var TestUtil2 = Java.type('com.salama.service.script.util.junittest.util.TestUtil2');"
                    + " TestUtil2.testStrList();"
                    );
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }
    
    public void test_List() {
        try {
            final ScriptEngine engine = createEngine();

            testScriptAndPrint(
                    engine, 
                    "var arr = testUtil.testStrArray();"
                    + "print('arr.length:' + arr.length);"
                    + " arr[1];"
                    );
            
            testScriptAndPrint(
                    engine, 
                    "var list = testUtil.testStrList();"
                    + "print('list.length:' + list.length);"
                    + "print('list.size():' + list.size());"
                    + " list[1];"
                    );
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }
    
    public void test_Map() {
        try {
            final ScriptEngine engine = createEngine();

            testScriptAndPrint(
                    engine, 
                    "var map = testUtil.testComplexMap();"
                    + " map['k1'];"
                    );
            testScriptAndPrint(
                    engine, 
                    "var map = testUtil.testComplexMap();"
                    + " map['m1'];"
                    );
            testScriptAndPrint(
                    engine, 
                    "var map = testUtil.testComplexMap();"
                    + " map['m1']['m2']['k3'];"
                    );
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }
    
    public void test_BaseTypes() {
        try {
            final ScriptEngine engine = createEngine();

            testScriptAndPrint(engine, "testUtil.ymdHms()");

            testScriptAndPrint(engine, "testUtil.utc()");

            testScriptAndPrint(engine, "testUtil.testPlainMap()");

            testScriptAndPrint(engine, "testUtil.testComplexMap()");

            testScriptAndPrint(engine, "testUtil.testStrArray()");
            
            testScriptAndPrint(engine, "testUtil.testStrList()");
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    private static Object testScriptAndPrint(
            final ScriptEngine engine,
            String script
            ) throws ScriptException {
        Object retVal = engine.eval(script);
        printObj(script + " -> ", retVal);
        
        return retVal;
    }
    
    public static void printObj(
            String msgPrefix,
            Object obj
            ) {
        System.out.println(
                msgPrefix
                + " \tobj.class:" + (obj == null ? " null" : obj.getClass().getName()) 
                + " \tobj: " + obj
        );
    }

    private static ScriptEngine createEngine() {
        final ScriptEngineManager engineManager = ScriptEngineUtil.createEngineManager();
        //global variables -----------------------------
        engineManager.put("testUtil", new TestUtil());
        
        final ScriptEngine engine = ScriptEngineUtil.createScriptEngine(engineManager);
        
        System.out.println("engineManager.class:" + engineManager.getClass());
        System.out.println("engine.class:" + engine.getClass());
        return engine;
    }
}
