package com.salama.service.script.test.junittest;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import javax.script.Compilable;
import javax.script.CompiledScript;
import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

import org.junit.Test;

public class TestScriptFunc {

    @Test
    public void test_2() {
        try {
            final ScriptEngineManager engineManager = ScriptEngineUtil.createEngineManager();
            final ScriptEngine engine = ScriptEngineUtil.createScriptEngine(engineManager);
            final Invocable jsInvoke = (Invocable) engine;
            final Compilable jsCompiler = (Compilable) engine;

            String script = ""
                    + "Collectors = Java.type('java.util.stream.Collectors');\n"
                    + "Test1 = new (\n"
                    + "function() {\n"
                    + "  this.test = function(list) {\n"
                    + "    var result = list.stream()\n"
                    + "      .map(function(e) {\n"
                    + "         return {e0: e.hashCode() % 4, e1: e};\n"
                    + "      })\n"
                    + "      .collect(Collectors.groupingBy(\n"
                    + "                 function(e) {\n"
                    + "                   return e.e0; \n"
                    + "                 }\n"
                    + "      ))\n"
                    + "      ; \n"
                    + "    print('result:' + result);\n"
                    + "  };\n"
                    + "  //as private method"
                    + "  function test2() {\n"
                    + "    print('test2 -----');\n"
                    + "  }\n"
                    + "}"
                    + ");"
                    ;
            
            List<String> list = new ArrayList<String>();
            for(int i = 0; i < 1000; i++) {
                list.add("test_" + i);
            }

            CompiledScript compiledScript = jsCompiler.compile(script);
            Object jsObj = compiledScript.eval();

            jsInvoke.invokeMethod(jsObj, "test", list);
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }
    
    public void test_1() {
        try {
            final ScriptEngineManager engineManager = ScriptEngineUtil.createEngineManager();
            final ScriptEngine engine = ScriptEngineUtil.createScriptEngine(engineManager);
            final Invocable jsInvoke = (Invocable) engine;
            final Compilable jsCompiler = (Compilable) engine;

            String script = ""
                    + "Test1 = new ("
                    + "function() {\n"
                    + "  this.test = function(list) {\n"
                    + "    var result = list.stream()\n"
                    + "      .map(function(e) {return e + ',';})\n"
                    + "      .reduce(function(e1, e2) {return e1 + e2})\n"
                    + "      .get(); \n"
                    + "    print('result:' + result);\n"
                    + "  };\n"
                    + "}"
                    + ");"
                    ;
            
            List<String> list = Arrays.asList("a", "b", "c", "d");
            String result = list.stream()
            .map(e -> (e + ","))
            .reduce((e1, e2) -> (e1 + e2))
            .get()
            ;
            System.out.println(result);
            
            CompiledScript compiledScript = jsCompiler.compile(script);
            Object jsObj = compiledScript.eval();
            jsInvoke.invokeMethod(jsObj, "test", list);
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }
    
    
}
