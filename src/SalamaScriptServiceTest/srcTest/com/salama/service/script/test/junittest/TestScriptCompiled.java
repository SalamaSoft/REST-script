package com.salama.service.script.test.junittest;

import javax.script.Compilable;
import javax.script.CompiledScript;
import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

import org.junit.Test;

public class TestScriptCompiled {

    public void test_3() {
        try {
            final ScriptEngine engine = createEngine();
            final Invocable jsInvoke = (Invocable) engine;
            final Compilable jsCompiler = (Compilable) engine;

            {
                String script = "TestCompiled1 = "
                        + "{"
                        + "  getName: function() {" 
                        + "    return 'testCompile1';"
                        + "  },"
                        + "  doJob: function(jobName) {"
                        + "    print(this.getName() + ' doJob -> ' + jobName);"
                        + "  },"
                        + "};"
                        //+ "TestCompiled1;"
                        ;
                CompiledScript compiledScript = jsCompiler.compile(script);
                Object jsObj = compiledScript.eval();
                
                //Object jsObj = engine.eval("TestCompiled1;");
                jsInvoke.invokeMethod(jsObj, "doJob", "test测试111 ---");
                
            }
            {
                String script = "TestCompiled2 = "
                        + "{"
                        + "  getName: function() {" 
                        + "    return 'testCompile2';"
                        + "  },"
                        + "  doJob2: function(jobName) {"
                        + "    TestCompiled1.doJob(jobName);"
                        + "  },"
                        + "}"
                        + ""
                        ;
                CompiledScript compiledScript = jsCompiler.compile(script);
                compiledScript.eval();
                
                Object jsObj = engine.eval("TestCompiled2;");
                jsInvoke.invokeMethod(jsObj, "doJob2", "test测试222 ---");
                
            }
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    @Test
    public void test_2() {
        try {
            final ScriptEngine engine = createEngine();
            final Invocable jsInvoke = (Invocable) engine;
            final Compilable jsCompiler = (Compilable) engine;

            {
                String script = ""
                        + "function TestCompiled1() {"
                        + "  this.getName = function() {" 
                        + "    return 'testCompile1';"
                        + "  };"
                        + "  this.doJob = function(jobName) {"
                        + "    print(this.getName() + ' -> ' + jobName);"
                        + "  };"
                        + "  this.doJob2 = function(jobName) {"
                        + "    print(this.getName() + ' doJob2 -> ' + jobName);"
                        + "  };"
                        + "}"
                        + ""
                        //+ "new TestCompiled1();"
                        ;
                CompiledScript compiledScript = jsCompiler.compile(script);
                compiledScript.eval();
                
                compiledScript.getEngine().eval("(new TestCompiled1()).doJob2('test测试222')");
                engine.eval("(new TestCompiled1()).doJob2('test测试333')");
                
                Object jsObj = engine.eval("new TestCompiled1();");
                TestScriptJavaExtend.printClassInfo(jsObj.getClass());
                
                ITest1 test1 = jsInvoke.getInterface(jsObj, ITest1.class);
                test1.doJob("test测试111");
                
                jsInvoke.invokeMethod(jsObj, "doJob2", "test测试444");
            }

            {
                String script = ""
                        + "function TestCompiled2() {"
                        + "  this.getName = function() {" 
                        + "    return 'testCompile1';"
                        + "  };"
                        + "  this.doJob3 = function(jobName) {"
                        + "    print(this.getName() + ' doJob3 -> ' + jobName);"
                        + "  };"
                        + "}"
                        + ""
                        ;
                CompiledScript compiledScript = jsCompiler.compile(script);
                compiledScript.eval();
                
                engine.eval("(new TestCompiled2()).doJob3('test测试222')");
                
                Object jsObj = compiledScript.getEngine().eval("new TestCompiled2();");
                jsInvoke.invokeMethod(jsObj, "doJob3", "test测试333 ---");
            }
            
            //test type override
            {
                String script = ""
                        + "function TestCompiled1() {"
                        + "  this.getName = function() {" 
                        + "    return 'testCompile1_override';"
                        + "  };"
                        + "  this.doJob = function(jobName) {"
                        + "    print(this.getName() + ' -> ' + jobName);"
                        + "  };"
                        + "  this.doJob3 = function(jobName) {"
                        + "    print(this.getName() + ' doJob3 -> ' + jobName);"
                        + "  };"
                        + "};"
                        + "$TestCompiled1 = new TestCompiled1();"
                        ;
                CompiledScript compiledScript = jsCompiler.compile(script);
                Object jsObj = compiledScript.eval();
                
                //Object jsObj = engine.eval("new TestCompiled1();");
                jsInvoke.invokeMethod(jsObj, "doJob3", "test测试555 ---");
                engine.eval("$TestCompiled1.doJob3('test测试666 ---');");
                
                jsInvoke.invokeMethod(jsObj, "doJob", "test测试555 ---");
                engine.eval("$TestCompiled1.doJob('test测试666 ---');");
                
                engine.eval("(new TestCompiled1()).doJob('test测试555--->')");
                
            }
            
            //invoke the method in other script
            {
                String script = ""
                        + "function TestCompiled4() {"
                        + "  this.doJob = function(jobName) {"
                        + "    (new TestCompiled1()).doJob3(jobName);"
                        + "  };"
                        + "}"
                        + ""
                        ;
                CompiledScript compiledScript = jsCompiler.compile(script);
                compiledScript.eval();
                
                Object jsObj = engine.eval("new TestCompiled4();");
                jsInvoke.invokeMethod(jsObj, "doJob", "test测试777 ---");
            }

            {
                String script = ""
                        + "TestCompile5 = new ("
                        + "function() {"
                        + "  this.getName = function() {" 
                        + "    return 'testCompile5';"
                        + "  };"
                        + "  this.doJob = function(jobName) {"
                        + "    print(this.getName() + ' doJob -> ' + jobName);"
                        + "  };"
                        + "}"
                        + ")"
                        ;
                CompiledScript compiledScript = jsCompiler.compile(script);
                Object jsObj = compiledScript.eval();
                jsInvoke.invokeMethod(jsObj, "doJob", "test测试888 ---");
                
                engine.eval("TestCompile5.doJob('test测试999 ---');");
            }
            
            {
                engine.eval("(new TestCompiled4()).doJob('test测试>>>>>>>>>>>>>')");
                engine.eval("TestCompile5.doJob('test测试>>>>>>>>>>>>>', 'extraParam')");
            }
            
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }
    
    public void test_1() {
        try {
            final ScriptEngine engine = createEngine();
            final Invocable jsInvoke = (Invocable) engine;
            Compilable jsCompiler = (Compilable) engine;
            
            String script = ""
                    + "function getName() {"
                    + "  return 'testCompile1';"
                    + "}"
                    + ""
                    + "function doJob(jobName) {"
                    + "  print(getName() + ' -> ' + jobName);"
                    + "}"
                    
                    + "function doJob2(jobName) {"
                    + "  print(getName() + ' doJob2 -> ' + jobName);"
                    + "}"
                    ;
            CompiledScript compiledScript = jsCompiler.compile(script);
            TestScriptJavaExtend.printClassInfo(compiledScript.getClass());
    
            Object jsObj = compiledScript.eval();
            compiledScript.getEngine().eval("doJob2('test测试111');");

            ITest1 test1 = jsInvoke.getInterface(jsObj, ITest1.class);
            test1.doJob("test测试111");
        } catch (Throwable e) {
            e.printStackTrace();
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
