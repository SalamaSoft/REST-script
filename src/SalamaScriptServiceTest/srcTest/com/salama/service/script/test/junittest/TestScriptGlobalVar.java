package com.salama.service.script.test.junittest;

import java.io.File;

import javax.script.Bindings;
import javax.script.CompiledScript;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

import org.junit.Test;

public class TestScriptGlobalVar {

    @Test
    public void test_1() {
        try {
            final ScriptEngineManager engineManager = ScriptEngineUtil.createEngineManager();
            //global vars
            engineManager.put("$TestUtil", new TestUtil());
            /* This will cause error
            {
                String script = ""
                        + "var javaType = Java.type('" + TestUtil2.class.getName() + "');"
                        + "javaType;"
                        ;
                final ScriptEngine engine = ScriptEngineUtil.createScriptEngine(engineManager);
                Object jsObj = ScriptEngineUtil.compileScript(engine, script);
                
                engineManager.put("$TestUtil2", jsObj);
            }
            */
            

            {
                String script = ""
                        + "print('TestGlobalVars ---> $TestUtil.utc():' + $TestUtil.utc());\n"
                        ; 
                final ScriptEngine engine = ScriptEngineUtil.createScriptEngine(engineManager);
                engine.eval(script);

                //Error occurs after clearing the global variable
                engineManager.put("$TestUtil", null);
                engine.eval(script);
            }

        } catch (Throwable e) {
            e.printStackTrace();
        }
    }
    
}

