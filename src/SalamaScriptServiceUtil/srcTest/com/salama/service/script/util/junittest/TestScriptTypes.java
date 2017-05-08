package com.salama.service.script.util.junittest;

import java.util.Map;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import org.junit.Test;

import com.salama.service.script.util.junittest.util.TestUtil;
import com.salama.service.script.util.script.ScriptEngineUtil;

public class TestScriptTypes {
    
    @Test
    public void test_4() {
        try {
            final ScriptEngine engine = createEngine();

            Object retVal = testScriptAndPrint(
                    engine, 
                    "var obj = {k1: 'v1', k2: 'v2', 'k3':100};"
                    + " JSON.stringfy(obj);"
                    );
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }
    
    public void test_3() {
        try {
            final ScriptEngine engine = createEngine();

            testScriptAndPrint(
                    engine, 
                    "var arr = $testUtil.testStrArray();"
                    + " arr[1];"
                    );
            
            testScriptAndPrint(
                    engine, 
                    "var list = $testUtil.testStrList();"
                    + " list[1];"
                    );
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }
    
    public void test_2() {
        try {
            final ScriptEngine engine = createEngine();

            testScriptAndPrint(
                    engine, 
                    "var map = $testUtil.testComplexMap();"
                    + " map['k1'];"
                    );
            testScriptAndPrint(
                    engine, 
                    "var map = $testUtil.testComplexMap();"
                    + " map['m1'];"
                    );
            testScriptAndPrint(
                    engine, 
                    "var map = $testUtil.testComplexMap();"
                    + " map['m1']['m2']['k3'];"
                    );
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }
    
    public void test_1() {
        try {
            final ScriptEngine engine = createEngine();

            testScriptAndPrint(engine, "$testUtil.ymdHms()");

            testScriptAndPrint(engine, "$testUtil.utc()");

            testScriptAndPrint(engine, "$testUtil.testPlainMap()");

            testScriptAndPrint(engine, "$testUtil.testComplexMap()");

            testScriptAndPrint(engine, "$testUtil.testStrArray()");
            
            testScriptAndPrint(engine, "$testUtil.testStrList()");
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
    
    private static void printObj(
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
        engineManager.put("$testUtil", new TestUtil());
        
        final ScriptEngine engine = ScriptEngineUtil.createScriptEngine(engineManager);
        
        System.out.println("engineManager.class:" + engineManager.getClass());
        System.out.println("engine.class:" + engine.getClass());
        return engine;
    }
}
