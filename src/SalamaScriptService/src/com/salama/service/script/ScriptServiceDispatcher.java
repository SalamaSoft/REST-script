package com.salama.service.script;

import java.io.Reader;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

import javax.script.Compilable;
import javax.script.CompiledScript;
import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import org.apache.log4j.Logger;

import com.salama.service.script.core.IScriptService;
import com.salama.service.script.core.IScriptSourceProvider;
import com.salama.service.script.core.IScriptSourceWatcher;

class ScriptServiceDispatcher {
    private final static Logger logger = Logger.getLogger(ScriptServiceDispatcher.class);

    private final String _scriptEngineName;
    private final ScriptEngineManager _scriptEngineManager;
    private final IScriptSourceProvider _scriptSourceProvider;
    //key:$app    value:ScriptSourceManager
    private final ConcurrentHashMap<String, ScriptSourceManager> _scriptManagerMap = new ConcurrentHashMap<String, ScriptSourceManager>();
    private final ReentrantLock _lockForScriptManager = new ReentrantLock();
    
    private final IScriptSourceWatcher _scriptSourceWatcher = new IScriptSourceWatcher() {

        @Override
        public void onGlobalVarUpdated(String varName, Object obj) {
            _scriptEngineManager.put(varName, obj);
            logger.info("onGlobalVarUpdated() -> " + varName);
        }

        @Override
        public void onGlobalVarDeleted(String varName) {
            _scriptEngineManager.put(varName, null);
            logger.info("onGlobalVarDeleted() -> " + varName);
        }

        @Override
        public String onScriptSourceUpdated(String app, Reader script) throws ScriptException {
            String serviceName = getScriptManager(app).updateScript(script);
            logger.info("onScriptSourceUpdated() -> app:" + app + " serviceName:" + serviceName);
            
            return serviceName;
        }

        @Override
        public void onScriptSourceDeleted(String app, String name) {
            getScriptManager(app).deleteScript(name);
            logger.info("onScriptSourceDeleted() -> app:" + app + " serviceName:" + name);
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

    public CompiledScript getCompiledScript(String app, String serviceName) {
        return getScriptManager(app).getCompiledScript(serviceName);
    }
    
    private ScriptSourceManager getScriptManager(String app) {
        ScriptSourceManager scriptManager = _scriptManagerMap.get(app);
        if(scriptManager != null) {
            return scriptManager;
        }
        
        _lockForScriptManager.lock();
        try {
            scriptManager = _scriptManagerMap.get(app);
            if(scriptManager != null) {
                return scriptManager;
            }
            
            scriptManager = new ScriptSourceManager();
            _scriptManagerMap.put(app, scriptManager);
            return scriptManager;
        } finally {
            _lockForScriptManager.unlock();
        }
    }
    
    private class ScriptSourceManager {
        private final ScriptEngine _engine;
        
        //key: $serviceName    value: script object
        private final ConcurrentHashMap<String, CompiledScript> _scriptMap = new ConcurrentHashMap<>();
        
        public ScriptSourceManager() {
            _engine = createScriptEngine(_scriptEngineManager);
        }
        
        public String updateScript(Reader script) throws ScriptException {
            final CompiledScript compiledScript = ((Compilable) _engine).compile(script);
            Object jsObj = compiledScript.eval();
            
            IScriptService service = ((Invocable) _engine).getInterface(jsObj, IScriptService.class);
            String serviceName = service.serviceName();

            _scriptMap.put(serviceName, compiledScript);
            return serviceName;
        }
        
        public void deleteScript(String serviceName) {
            _scriptMap.remove(serviceName);
        }
        
        public CompiledScript getCompiledScript(String serviceName) {
            return _scriptMap.get(serviceName);
        }
    }
    
    public ScriptEngine createScriptEngine(ScriptEngineManager engineManager) {
        return engineManager.getEngineByName(_scriptEngineName);
    }
    
}
