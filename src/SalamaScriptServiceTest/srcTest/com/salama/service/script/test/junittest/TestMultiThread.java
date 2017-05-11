package com.salama.service.script.test.junittest;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.script.Compilable;
import javax.script.CompiledScript;
import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

import org.junit.Test;

public class TestMultiThread {

    @Test
    public void test_2() {
        String regex = "[a-zA-Z0-9\\-_\\.]+";
        Pattern pattern = Pattern.compile(regex);
        
        String input = "aaa.BB-xc_";
        Matcher matcher = pattern.matcher(input);
        boolean match = matcher.matches();
        System.out.println(
                "match[" + input + "] ->"
                + " match:" + match 
                + " hitEnd:" + matcher.hitEnd()
                + " matchStart:" + matcher.start()
                + " matchEnd:" + matcher.end()
                + " input.len:" + input.length()
                );
    }
    
    public void test_1() {
        try {
            final ScriptEngineManager engineManager = ScriptEngineUtil.createEngineManager();
            final ScriptEngine engine = ScriptEngineUtil.createScriptEngine(engineManager);
            final Invocable jsInvoke = (Invocable) engine;
            final Compilable jsCompiler = (Compilable) engine;

            String script = ""
                    + "var Random = Java.type('java.util.Random');\n"
                    + "Test = new (\n"
                    + "function() {\n"
                    + "  var _rand = new Random();\n"
                    + "  \n"
                    + "  this.run = function(jobName) {\n"
                    + "  try {\n"
                    + "    var sum = 0;\n"
                    + "    for(var i = 0; i < 1000; i++) {\n"
                    + "      var n = _rand.nextInt(100);\n"
                    + "      sum += n;\n"
                    + "    }\n"
                    + "    //print(jobName + ' -----> sum:' + sum);\n"
                    + "    return sum;\n"
                    + "  } catch(e) {print('error -> ' + e); return 0;} \n"
                    + " };\n"
                    + "}\n"
                    + ");"
                    ; 
            //Object jsObj = engine.eval(script);
            final CompiledScript compiledScript = jsCompiler.compile(script);
            Object jsObj = compiledScript.eval();
            Object retVal = jsInvoke.invokeMethod(jsObj, "run", "test run");
            System.out.println(" test run:" + retVal);

            int totalIterate = 80000;
            int threadMax = 8;
            final int jobIter = totalIterate / threadMax;
            final AtomicLong iterCounter = new AtomicLong();
            ExecutorService threadPool = Executors.newFixedThreadPool(threadMax);
            for(int i = 0; i < threadMax; i++) {
                final String jobName = "job_" + i;
                threadPool.submit(new Runnable() {
                    
                    @Override
                    public void run() {
                        try {
                            //System.out.println(jobName + " start ------>");
                            final Object jsObj2 = compiledScript.eval();
                            long beginTime = System.currentTimeMillis();
                            
                            for(int i = 0; i < jobIter; i++) {
                                long beginTime2 = System.currentTimeMillis();
                                
                                //Object retVal = engine.eval("Test.run()");
                                //Object retVal = jsInvoke.invokeMethod(jsObj, "run", jobName);
                                
                                //this way is faster than others above
                                Object retVal = jsInvoke.invokeMethod(jsObj2, "run", jobName);
                                
                                iterCounter.incrementAndGet();                                
                                //System.out.println(jobName + "[" + i + "] --> cost(ms):" + (System.currentTimeMillis() - beginTime2));
                            }
                            
                            System.out.println(jobName + " iterCounter:" + iterCounter.get() + " --> cost(ms):" + (System.currentTimeMillis() - beginTime));
                        } catch (Throwable e) {
                            e.printStackTrace();
                        }
                    }
                });
            }
            
            threadPool.awaitTermination(20, TimeUnit.SECONDS);
            threadPool.shutdown();
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }
    
}
