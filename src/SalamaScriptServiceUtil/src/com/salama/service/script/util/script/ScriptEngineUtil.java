package com.salama.service.script.util.script;

import javax.script.Compilable;
import javax.script.CompiledScript;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

/**
 * 
 * @author beef-liu
 *
 */
public class ScriptEngineUtil {
    private final static String DEFAULT_ENGINE_NAME = "nashorn";


    public static CompiledScript compileScript(
            ScriptEngine engine, String script
            ) throws ScriptException {
        Compilable compEngine = (Compilable) engine;
        CompiledScript compiledScript = compEngine.compile(script);
        return compiledScript;
    }
    
    public static ScriptEngine createScriptEngine(
            ScriptEngineManager engineManager) {
        return engineManager.getEngineByName(DEFAULT_ENGINE_NAME);
    }
    
    public static ScriptEngineManager createEngineManager() {
        return new ScriptEngineManager();
    }
    
}
