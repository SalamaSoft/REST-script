package com.salama.service.script.test.junittest;

import java.lang.reflect.Method;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

import org.junit.Test;

public class TestScriptJavaExtend {

    @Test
    public void test_Java() {
        try {
            final ScriptEngine engine = createEngine();
            final Invocable jsInvoke = (Invocable) engine;
            
            Object java = engine.eval("Java");
            printClassInfo(java.getClass());
        } catch (Throwable e) {
            e.printStackTrace();
        }        
    }
    
    public void test_2() {
        try {
            final ScriptEngine engine = createEngine();
            final Invocable jsInvoke = (Invocable) engine;
            
            String script1 = ""
                    + "var MyImp1 = Java.extend(Java.type('com.salama.service.script.util.junittest.ITest1'), {"
                    + "  getName: function() {"
                    + "    return 'MyImp1';"
                    + "  }, doJob: function(jobName) {"
                    + "    print(this.getName() + ' --> jobName:' + jobName);"
                    + "  }, doJob2: function(jobName) {"
                    + "    print(this.getName() + ' doJob2() --> jobName:' + jobName);"
                    + "  }"
                    + "});"
                    ;
            engine.eval(script1);
            Object jsObj = engine.eval("var imp1 = new MyImp1();  imp1.doJob('testDoJob测试aa');");
            //printClassInfo(jsObj.getClass());
            TestScriptTypes.printObj("1 ---> ", jsObj);

            jsObj = engine.eval("imp1.doJob2('testDoJob测试aa');");
            //printClassInfo(jsObj.getClass());
            TestScriptTypes.printObj("2 ---> ", jsObj);
            
            
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }
    
    public void test_1() {
        try {
            final ScriptEngine engine = createEngine();
            final Invocable jsInvoke = (Invocable) engine;
            
            String script1 = ""
                    + "function test1(p0, p1, p2) {"
                    + "  print('p0:' + p0 + ' p1:' + p1 + ' p2:' + p2);"
                    + "}"
                    ;
            String script2 = ""
                    + "function test2(p0, p1) {"
                    + "  print('p0:' + p0 + ' p1:' + p1);"
                    + "}"
                    ;
            engine.eval(script1);
            engine.eval("test1()");
            jsInvoke.invokeFunction("test1", "v0", "v1", "v2");
            
            engine.eval(script2);
            engine.eval("test2()");
            engine.eval("test1()");
            jsInvoke.invokeFunction("test2", "v0", "v1", "v2");
            jsInvoke.invokeFunction("test1", "v0", "v1", "v2");
            
            //printClassInfo(scriptObj.getClass());
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    public static void printClassInfo(Class<?> cls) {
        System.out.println("Class info ------> " + cls.getName());
        
        {
            Method[] methods = cls.getDeclaredMethods();
            for(Method m : methods) {
                System.out.println(
                        "declared method -> " + m.getName() 
                        + " ParamCount:" + m.getParameterCount()
                        + " ReturnType:" + m.getReturnType()
                        );
            }
            
        }
        
        {
            Method[] methods = cls.getMethods();
            for(Method m : methods) {
                System.out.println(
                        "method -> " + m.getName() 
                        + " ParamCount:" + m.getParameterCount()
                        + " ReturnType:" + m.getReturnType()
                        );
            }
        }
    }
    
    private static ScriptEngine createEngine() {
        final ScriptEngineManager engineManager = ScriptEngineUtil.createEngineManager();
        final ScriptEngine engine = ScriptEngineUtil.createScriptEngine(engineManager);
        
        System.out.println("engineManager.class:" + engineManager.getClass());
        System.out.println("engine.class:" + engine.getClass());
        return engine;
    }
    
}
