package com.salama.service.script;

import java.io.Reader;
import java.util.concurrent.ConcurrentHashMap;

import javax.script.Compilable;
import javax.script.CompiledScript;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

import org.apache.log4j.Logger;

import com.salama.service.script.core.IScriptSourceProvider;
import com.salama.service.script.core.IScriptSourceWatcher;

class ScriptServiceDispatcher {
    private final static Logger logger = Logger.getLogger(ScriptServiceDispatcher.class);
    private final static String DEFAULT_ENGINE_NAME = "nashorn";

    private final String _scriptEngineName;
    private final ScriptEngineManager _scriptEngineManager;
    private final IScriptSourceProvider _scriptSourceProvider;
    //key:$app    value:ScriptSourceManager
    private final ConcurrentHashMap<String, ScriptSourceManager> _scriptSourceManager = new ConcurrentHashMap<String, ScriptSourceManager>();
    
    private final IScriptSourceWatcher _scriptSourceWatcher = new IScriptSourceWatcher() {

        @Override
        public void onGlobalVarUpdated(String varName, Object obj) {
            logger.info("onGlobalVarUpdated() -> " + varName);
            _scriptEngineManager.put(varName, obj);
        }

        @Override
        public void onGlobalVarDeleted(String varName) {
            logger.info("onGlobalVarDeleted() -> " + varName);
            _scriptEngineManager.put(varName, new Object());
        }

        @Override
        public void onScriptSourceUpdated(String app, Reader script) {
            // TODO Auto-generated method stub
            
        }

        @Override
        public void onScriptSourceDeleted(String app, String name) {
            // TODO Auto-generated method stub
            
        }
        
    }; 

    public ScriptServiceDispatcher(
            String scriptEngineName, IScriptSourceProvider scriptSourceProvider
            ) {
        _scriptEngineName = scriptEngineName;
        _scriptSourceProvider = scriptSourceProvider;
        
        _scriptEngineManager = new ScriptEngineManager();
        
        _scriptSourceProvider.addWatcher(_scriptSourceWatcher);
    }
    
    private class ScriptSourceManager {
        private final ScriptEngine _engine;
        
        //key: $serviceName    value: script object
        private final ConcurrentHashMap<String, CompiledScript> _scriptObjMap = new ConcurrentHashMap<>();
        
        public ScriptSourceManager() {
        }
        
    }
    
    public ScriptEngine createScriptEngine(ScriptEngineManager engineManager) {
        return engineManager.getEngineByName(_scriptEngineName);
    }
    
}
