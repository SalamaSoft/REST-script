package com.salama.service.script.sourcecontainer;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

import javax.script.Compilable;
import javax.script.CompiledScript;
import javax.script.Invocable;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import org.apache.log4j.Logger;

import com.salama.service.script.core.IConfigLocationResolver;
import com.salama.service.script.core.IScriptContext;
import com.salama.service.script.core.IScriptService;
import com.salama.service.script.core.IScriptSourceContainer;
import com.salama.service.script.core.IServiceNameVerifier;
import com.salama.service.script.core.ServiceTarget;

public class ScriptSourceContainer implements IScriptSourceContainer {
    private final static Logger logger = Logger.getLogger(ScriptSourceContainer.class);
    
    public final static String[] Resource_scripts_ForDefaultGlobalVars = new String[] {
            "/com/salama/service/script/util/json.js", 
            "/com/salama/service/script/util/xml.js",
    };
    
    public final static String Script_GetApp = ""
            + "function $getApp() {\n"
            + "    return '$app';\n"
            + "}\n";
    
    private String _scriptEngineName;
    private IServiceNameVerifier _serviceNameVerifier;
    private IConfigLocationResolver _configLocationResolver;

    private ScriptEngineManager _scriptEngineManager;
    private ScriptEngine _defaultScriptEngine;
    private List<String> _sortedScriptContextNameList;

    //key:$app    value:ScriptSourceManager
    private final ConcurrentHashMap<String, ScriptSourceManager> _scriptSourceManagerMap = new ConcurrentHashMap<String, ScriptSourceManager>();
    private final ReentrantLock _lockForScriptSourceManager = new ReentrantLock();

    @Override
    public void init(String scriptEngineName, IServiceNameVerifier serviceNameVerifier, IConfigLocationResolver configLocationResolver) {
        _scriptEngineName = scriptEngineName;
        if(_scriptEngineName == null || _scriptEngineName.trim().length() == 0) {
            _scriptEngineName = "nashorn";
        }
        
        _serviceNameVerifier = serviceNameVerifier;
        _configLocationResolver = configLocationResolver;

        //init script engine
        _scriptEngineManager = new ScriptEngineManager();
        _defaultScriptEngine = createScriptEngine(_scriptEngineManager);
        
        _sortedScriptContextNameList = new ArrayList<>();

        //init default global vars ------
        loadDefaultGlobalVars();
    }

    @Override
    public void close() throws IOException {
        //destroy engine Objects
        try {
            for(Entry<String, ScriptSourceManager> sourceManagerEntry : _scriptSourceManagerMap.entrySet()) {
                try {
                    final String app = sourceManagerEntry.getKey();
                    
                    /*
                    for(Entry<String, Object> objEntry : sourceManagerEntry.getValue()
                            .getEngine().getBindings(ScriptContext.ENGINE_SCOPE).entrySet()) {
                            */
                    for(int i = sourceManagerEntry.getValue().getSortedScriptContextNameList().size() - 1; i >= 0; i--) {
                        /*
                        final String serviceName = objEntry.getKey();
                        final Object obj = objEntry.getValue();
                        */
                        final String serviceName =  sourceManagerEntry.getValue().getSortedScriptContextNameList().get(i);
                        final Object obj = sourceManagerEntry.getValue().getEngine().get(serviceName); 
                        if(obj == null) {
                            continue;
                        }
                        
                        if(IScriptContext.class.isAssignableFrom(obj.getClass())) {
                            //destroy old one
                            ((IScriptContext) obj).destroy();
                            logger.info("JavaObj destroyed ->"
                                    + " app[" + app + "] varName[" + serviceName + "]" 
                                    );
                        } else {
                            IScriptContext scriptContext = null;
                            try {
                                scriptContext = jsObjToInterface(
                                        (Invocable)sourceManagerEntry.getValue().getEngine(), 
                                        obj, 
                                        IScriptContext.class
                                        );
                            } catch (Throwable e) {
                                logger.info("ScriptService have no destroy method."
                                        + " app[" + app + "] serviceName[" + serviceName + "]"
                                        );
                            }
                            
                            if(scriptContext != null) {
                                scriptContext.destroy();
                                logger.info("ScriptContext destroyed ->"
                                        + " app[" + app + "] serviceName:[" + serviceName + "]" 
                                        );
                            }
                        }
                    }
                } catch(Throwable e) {
                    logger.error(null, e);
                }
            } 
        } catch(Throwable e) {
            logger.error(null, e);
        }
        
        //destroy global objects
        //for(Entry<String, Object> objEntry : _scriptEngineManager.getBindings().entrySet()) {
        for(int i = _sortedScriptContextNameList.size() - 1; i >= 0; i--) {
            try {
                /*
                final String varName = objEntry.getKey();
                final Object obj = objEntry.getValue();
                */
                final String varName = _sortedScriptContextNameList.get(i);
                final Object obj = _scriptEngineManager.get(varName);
                
                if(obj == null) {
                    continue;
                }
                
                if(destroyJavaObj(obj)) {
                    logger.info("JavaObj destroyed ->"
                            + " app[null] varName[" + varName + "]" 
                            );
                }
                
                logger.info("JavaObj destroyed ->"
                        + " app[null] varName[" + varName + "]" 
                        );
            } catch(Throwable e) {
                logger.error(null, e);
            }
        } 
    }

    @Override
    public CompiledScript findCompiledScript(ServiceTarget target) {
        return getScriptSourceManager(target.app).getCompiledScript(target.serviceName);
    }
    
    @Override
    public void onInitLoadJavaObj(List<InitLoadJavaEntry> initLoadEntries) {
        for(InitLoadJavaEntry entry : initLoadEntries) {
            onJavaObjUpdated(entry.getApp(), entry.getVarName(), entry.getObj(), entry.getConfig());
        }
    }

    @Override
    public void onJavaObjUpdated(String app, String varName, Object obj, Reader config) {
        if(updateJavaObj(app, varName, obj, config)) {
            logger.info("onJavaObjUpdated() succeeded"
                    + " app[" + app + "]"
                    + " var[" + varName + "]"
                    + " -> " + obj.getClass().getName()
                    );
        }
    }
    
    @Override
    public void onJavaObjDeleted(String app, String varName) {
        deleteJavaObj(app, varName);
    }
    
    @Override
    public void onInitLoadScriptSource(List<InitLoadScriptEntry> initLoadEntries) {
        for(InitLoadScriptEntry entry : initLoadEntries) {
            
        }
    }
    
    @Override
    public String onScriptSourceUpdated(String app, Reader script, Reader config) throws ScriptException {
        try {
            String serviceName = updateScriptSource(app, script, config);
            logger.info("onScriptSourceUpdated() -> app:" + app + " serviceName:" + serviceName);
            return serviceName;
        } catch (NoSuchMethodException e) {
            throw new RuntimeException("Error in updateScriptSource!", e);
        }
    }

    @Override
    public void onScriptSourceDeleted(String app, String serviceName) {
        deleteScriptSource(app, serviceName);
        logger.info("onScriptSourceDeleted() -> app:" + app + " serviceName:" + serviceName);
    }
    
    private void loadDefaultGlobalVars() {
        Charset charset = Charset.forName("utf-8");        
        for(String resPath : Resource_scripts_ForDefaultGlobalVars) {
            try {
                InputStreamReader reader = new InputStreamReader(ScriptSourceContainer.class.getResourceAsStream(resPath), charset);
                try {
                    Object jsObj = _defaultScriptEngine.eval(reader);
                    IScriptService scriptService = ((Invocable) _defaultScriptEngine).getInterface(jsObj, IScriptService.class);
                    String name = scriptService.serviceName();
                    _scriptEngineManager.put(name, jsObj);
                } finally {
                    reader.close();
                }
            } catch(Throwable e) {
                logger.error("Error in loadDefaultGlobalVars! res:" + resPath, e);
            }
        }
    }
        
    private boolean updateJavaObj(String app, String varName, Object obj, Reader config) {
        final boolean isGlobal;
        try {
            final Object oldObj;
            if(isAppEmpty(app)) {
                isGlobal = true;
                oldObj = _scriptEngineManager.get(varName);
            } else {
                if(!_serviceNameVerifier.verifyFormatOfApp(app)) {
                    throw new RuntimeException("Invalid format of app:" + app);
                }
                
                isGlobal = false;
                oldObj = getScriptSourceManager(app).getEngine().get(varName);
            }
            
            boolean destroyInvoked = false;
            if(oldObj != null) {
                try {
                    if(destroyJavaObj(oldObj)) {
                        destroyInvoked = true;
                        logger.info("JavaObj destroyed ->"
                                + " app[" + app + "] varName[" + varName + "]" 
                                );
                    }
                } catch (Throwable e) {
                    logger.error(
                            "Error occurred in destroying JavaObj! ->"
                            + " app[" + app + "] varName[" + varName + "]", 
                            e
                            );
                }
            }

            if(reloadJavaObj(obj, config)) {
                logger.info("JavaObj reloaded ->"
                        + " app[" + app + "] varName[" + varName + "]" 
                        );
            }
            
            if(isGlobal) {
                _scriptEngineManager.put(varName, obj);
                
                //add into contextOrderList when updating at 1st time
                if(!destroyInvoked) {
                    _sortedScriptContextNameList.add(varName);
                }
            } else {
                getScriptSourceManager(app).getEngine().put(varName, obj);
                
                //add into contextOrderList when updating at 1st time
                if(!destroyInvoked) {
                    getScriptSourceManager(app).getSortedScriptContextNameList().add(varName);
                }
            }
            
            logger.info(
                    "javaObj updated."
                    + " app[" + app + "] varName:[" + varName + "]"
                    + " obj:" + obj
                    );
            return true;
        } catch (Throwable e) {
            logger.info("onJavaObjUpdated() succeeded"
                    + " app[" + app + "]"
                    + " var[" + varName + "]"
                    + " -> " + obj.getClass().getName()
                    );
            logger.error(
                    "Error occurred in updateJavaObj() ->"
                            + " app[" + app + "]"
                            + " var[" + varName + "]"
                            + " type:" + obj.getClass().getName(),
                    e
                    );
            return false;
        }
    }
    private void deleteJavaObj(String app, String varName) {
        final boolean isGlobal;
        final Object obj;
        if(isAppEmpty(app)) {
            isGlobal = true;
            obj = _scriptEngineManager.get(varName);
        } else {
            isGlobal = false;
            obj = getScriptSourceManager(app).getEngine().get(varName);
        }
        
        if(obj != null) {
            try {
                if(destroyJavaObj(obj)) {
                    logger.info("JavaObj destroyed ->"
                            + " app[" + app + "] varName[" + varName + "]" 
                            );
                }
            } catch (Throwable e) {
                logger.error(
                        "Error occurred in destroying JavaObj! ->"
                        + " app[" + app + "] varName[" + varName + "]", 
                        e
                        );
            }
        }
        
        if(isGlobal) {
            _scriptEngineManager.getBindings().remove(varName);
        } else {
            getScriptSourceManager(app).getEngine().getBindings(ScriptContext.ENGINE_SCOPE).remove(varName);
        }
        
        logger.info(
                "javaObj deleted."
                + " app[" + app + "] varName:[" + varName + "]"
                );
    }
    
    private boolean destroyJavaObj(Object obj) {
        if(IScriptContext.class.isAssignableFrom(obj.getClass())) {
            //destroy old one
            ((IScriptContext) obj).destroy();
            return true;
        }
        
        return false;
    }
    
    private boolean reloadJavaObj(Object obj, Reader config) throws IOException {
        if(IScriptContext.class.isAssignableFrom(obj.getClass())) {
            //destroy old one
            ((IScriptContext) obj).reload(config, _configLocationResolver);
            return true;
        }
        
        return false;
    }
    
    private String updateScriptSource(String app, Reader script, Reader config) throws ScriptException, NoSuchMethodException {
        final ScriptEngine engine;
        final boolean isGlobal;
        if(isAppEmpty(app)) {
            engine = _defaultScriptEngine;
            isGlobal = true;
        } else {
            if(!_serviceNameVerifier.verifyFormatOfApp(app)) {
                throw new RuntimeException("Invalid format of app:" + app);
            }
            
            engine = getScriptSourceManager(app).getEngine();
            isGlobal = false;
        }
        
        final CompiledScript compiledScript = ((Compilable) engine).compile(script);
        final Object jsObj = compiledScript.eval();
        if(jsObj == null) {
            logger.info(
                    "script source updated. (null returned by eval())"
                    + " app[" + app + "] serviceName:[null]"
                    + " compiledScript:" + compiledScript
                    );
            return null;
        }
        
        final IScriptService scriptService = jsObjToInterface((Invocable) engine, jsObj, IScriptService.class);
        if(scriptService == null) {
            logger.info(
                    "script source updated. (not implement IScriptService)"
                    + " app[" + app + "] serviceName:[null]"
                    + " compiledScript:" + compiledScript
                    );
            return null;
        }
        String serviceName = scriptService.serviceName();
        if(serviceName == null || serviceName.length() == 0) {
            logger.warn(
                    "script source updated."
                    + " app[" + app + "] serviceName:[" + serviceName + "]"
                    + " compiledScript:" + compiledScript
                    );
            return null;
        }
        
        if(!_serviceNameVerifier.verifyFormatOfServiceName(serviceName)) {
            throw new RuntimeException("Invalid format of serviceName:" + serviceName);
        }
        
        //destroy old one ---------------------------------------------
        boolean destroyInvoked = destroyWhenScriptContext(app, serviceName);

        //store the jsObj
        final IScriptContext scriptContext = jsObjToInterface((Invocable) engine, jsObj, IScriptContext.class);
        if(scriptContext != null) {
            //A ScriptContext will not be exposed as a service
            //reload
            try {
                scriptContext.reload(config, _configLocationResolver);
                logger.info("ScriptContext reloaded ->"
                        + " app[" + app + "] serviceName:[" + serviceName + "]" 
                        );
            } catch (Throwable e) {
                logger.error(
                        "Error occurred in reloading ScriptContext! ->"
                        + " app[" + app + "] serviceName:[" + serviceName + "]", 
                        e
                        );
            }
            
            //stored
            if(isGlobal) {
                _scriptEngineManager.put(serviceName, jsObj);
                
                //add into contextOrderList when updating at 1st time
                if(!destroyInvoked) {
                    _sortedScriptContextNameList.add(serviceName);
                }
            } else {
                getScriptSourceManager(app).getEngine().put(serviceName, jsObj);
                
                //add into contextOrderList when updating at 1st time
                if(!destroyInvoked) {
                    getScriptSourceManager(app).getSortedScriptContextNameList().add(serviceName);
                }
            }
        } else {
            if(isGlobal) {
                _scriptEngineManager.put(serviceName, jsObj);
            } else {
                //stored as a service
                getScriptSourceManager(app).putCompiledScript(scriptService.serviceName(), compiledScript);
            }
        }

        logger.info(
                "script source updated."
                + " app[" + app + "] serviceName:[" + serviceName + "]"
                + " compiledScript:" + compiledScript
                );
        return serviceName;
    }
    
    private void deleteScriptSource(String app, String serviceName) {
        destroyWhenScriptContext(app, serviceName);
        
        if(isAppEmpty(app)) {
            _scriptEngineManager.getBindings().remove(serviceName);
        } else {
            getScriptSourceManager(app).getEngine().getBindings(ScriptContext.ENGINE_SCOPE).remove(serviceName);
            getScriptSourceManager(app).deleteCompiledScript(serviceName);
        }
        
        logger.info(
                "script source deleted."
                + " app[" + app + "] serviceName:[" + serviceName + "]"
                );
    } 
    
    /**
     * 
     * @param app
     * @param serviceName
     * @return true: destroy() has been invoked
     */
    private boolean destroyWhenScriptContext(String app, String serviceName) {
        final Object oldJsObj;  
        final Invocable invoke;
        if(isAppEmpty(app)) {
            oldJsObj = _scriptEngineManager.get(serviceName);
            invoke = (Invocable) _defaultScriptEngine;
        } else {
            invoke = (Invocable) getScriptSourceManager(app).getEngine();
            oldJsObj = getScriptSourceManager(app).getEngine().get(serviceName);
        }
        
        if(oldJsObj != null) {
            //destroy the old one if need
            try {
                IScriptContext scriptContext = jsObjToInterface(invoke, oldJsObj, IScriptContext.class);
                if(scriptContext != null) {
                    scriptContext.destroy();
                    logger.info("ScriptContext destroyed ->"
                            + " app[" + app + "] serviceName:[" + serviceName + "]" 
                            );
                    return true;
                }
            } catch (Throwable e) {
                logger.error(
                        "Error occurred in destroying ScriptContext! ->"
                        + " app[" + app + "] serviceName:[" + serviceName + "]", 
                        e
                        );
            }
        }
        
        return false;
    }
    
    /*
    private static boolean scriptObjContainsMethodReload(Object jsObj) {
        return ((Map<String, Object>)jsObj).containsKey(IScriptContext.MethodName_reload);
    }

    private static boolean scriptObjContainsMethodDestroy(Object jsObj) {
        return ((Map<String, Object>)jsObj).containsKey(IScriptContext.MethodName_destroy);
    }
    
    private static boolean scriptObjContainsMethodServiceName(Object jsObj) {
        return ((Map<String, Object>)jsObj).containsKey(IScriptService.MethodName_serviceName);
    }
    */
    
    /*
    private static String getScriptServiceName(
            Invocable invoke,
            Object jsObj
            ) {
        IScriptService inst = jsObjToInterface(invoke, jsObj, IScriptService.class);
        if(inst == null) {
            return null;
        } else {
            return inst.serviceName();
        }
    }
    
    private boolean reloadScriptContext(
            Invocable invoke,
            Object jsObj,
            Reader config
            ) throws IOException {
        IScriptContext scriptContext = jsObjToInterface(invoke, jsObj, IScriptContext.class);
        if(scriptContext != null) {
            scriptContext.reload(config, _configLocationResolver);
            return true;
        }
        
        return false;
    }
    
    private boolean destroyScriptContext(
            Invocable invoke,
            Object jsObj
            ) {
        IScriptContext scriptContext = jsObjToInterface(invoke, jsObj, IScriptContext.class);
        if(scriptContext != null) {
            scriptContext.destroy();
            return true;
        }
        
        return false;
    }
    */
    
    private static <T> T jsObjToInterface(
            Invocable invoke,
            Object jsObj,
            Class<T> interfaceType
            ) {
        return invoke.getInterface(jsObj, interfaceType);
    }

    private static boolean isAppEmpty(String app) {
        return (app == null || app.length() == 0);
    }
    
    private ScriptSourceManager getScriptSourceManager(String app) {
        ScriptSourceManager scriptManager = _scriptSourceManagerMap.get(app);
        if(scriptManager != null) {
            return scriptManager;
        }
        
        _lockForScriptSourceManager.lock();
        try {
            scriptManager = _scriptSourceManagerMap.get(app);
            if(scriptManager != null) {
                return scriptManager;
            }
            
            scriptManager = new ScriptSourceManager(app);
            _scriptSourceManagerMap.put(app, scriptManager);
            return scriptManager;
        } finally {
            _lockForScriptSourceManager.unlock();
        }
    }
    
    private class ScriptSourceManager {
        private final ScriptEngine _engine;
        
        //key: $serviceName    value: script object
        private final ConcurrentHashMap<String, CompiledScript> _scriptMap = new ConcurrentHashMap<>();
        
        private final List<String> _sortedScriptContextNameList = new ArrayList<>();        
        
        public ScriptSourceManager(String app) {
            _engine = createScriptEngine(_scriptEngineManager);
            
            //init egine
            {
                String script = Script_GetApp.replace("$app", app);
                try {
                    _engine.eval(script);
                } catch (ScriptException e) {
                    logger.error("Error occurred in eval script:\n" + script, e);
                }
            }
        }
        
        public ScriptEngine getEngine() {
            return _engine;
        }
        
        public void putCompiledScript(String serviceName, CompiledScript compiledScript) throws ScriptException {
            _scriptMap.put(serviceName, compiledScript);
        }
        
        public void deleteCompiledScript(String serviceName) {
            _scriptMap.remove(serviceName);
        }
        
        public CompiledScript getCompiledScript(String serviceName) {
            return _scriptMap.get(serviceName);
        }
        
        public void addScriptContextName(String ctxName) {
            this._sortedScriptContextNameList.add(ctxName);
        }
        
        public List<String> getSortedScriptContextNameList() {
            return _sortedScriptContextNameList;
        }
    }
    
    
    private ScriptEngine createScriptEngine(ScriptEngineManager engineManager) {
        return engineManager.getEngineByName(_scriptEngineName);
    }
    
}
