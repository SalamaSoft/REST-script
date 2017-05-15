package com.salama.service.script;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.Charset;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

import javax.script.Compilable;
import javax.script.CompiledScript;
import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import org.apache.log4j.Logger;

import com.salama.service.core.net.RequestWrapper;
import com.salama.service.core.net.ResponseWrapper;
import com.salama.service.script.core.IScriptContext;
import com.salama.service.script.core.IScriptService;
import com.salama.service.script.core.IScriptServiceDispatcher;
import com.salama.service.script.core.IScriptSourceProvider;
import com.salama.service.script.core.IScriptSourceWatcher;
import com.salama.service.script.core.IServiceTargetFinder;
import com.salama.service.script.core.ServiceTarget;

public class ScriptServiceDispatcher implements IScriptServiceDispatcher {
    private final static Logger logger = Logger.getLogger(ScriptServiceDispatcher.class);
    
    public final static String[] Resource_scripts_ForDefaultGlobalVars = new String[] {
            "com/salama/service/script/resource/script/json.js", 
            "com/salama/service/script/resource/script/xml.js",
    };
    
    private final String _scriptEngineName;
    private final ScriptEngineManager _scriptEngineManager;
    
    private final IScriptSourceProvider _scriptSourceProvider;
    private final IServiceTargetFinder _serviceTargetFinder;
    
    //key:$app    value:ScriptSourceManager
    private final ConcurrentHashMap<String, ScriptSourceManager> _scriptSourceManagerMap = new ConcurrentHashMap<String, ScriptSourceManager>();
    private final ReentrantLock _lockForScriptSourceManager = new ReentrantLock();
    
    private final IScriptSourceWatcher _scriptSourceWatcher = new IScriptSourceWatcher() {

        @Override
        public void onJavaObjUpdated(String app, String varName, Object obj, String config) {
            if(app == null || app.length() == 0) {
                _scriptEngineManager.put(varName, obj);
            } else {
            }
            
            logger.info("onJavaObjUpdated()"
                    + " app:[" + app + "]"
                    + " var[" + varName + "]"
                    + " -> " + obj.getClass().getName()
                    );
        }

        @Override
        public void onJavaObjDeleted(String app, String varName) {
            // TODO Auto-generated method stub
            
        }
        
        @Override
        public void onGlobalVarDeleted(String varName) {
            _scriptEngineManager.put(varName, null);
            logger.info("onGlobalVarDeleted() -> " + varName);
        }

        @Override
        public String onScriptSourceUpdated(String app, Reader script, String config) throws ScriptException {
            if(!_serviceTargetFinder.verifyFormatOfApp(app)) {
                throw new RuntimeException("Invalid format of app:");
            }
            String serviceName = getScriptSourceManager(app).updateScript(script);
            logger.info("onScriptSourceUpdated() -> app:" + app + " serviceName:" + serviceName);
            
            return serviceName;
        }

        @Override
        public void onScriptSourceDeleted(String app, String name) {
            getScriptSourceManager(app).deleteScript(name);
            logger.info("onScriptSourceDeleted() -> app:" + app + " serviceName:" + name);
        }
        
    };
    
    @Override
    public IScriptSourceWatcher getScriptSourceWatcher() {
        return _scriptSourceWatcher;
    }
    
    @Override
    public IServiceTargetFinder getServiceTargetFinder() {
        return _serviceTargetFinder;
    }

    public ScriptServiceDispatcher(
            String scriptEngineName, 
            IScriptSourceProvider scriptSourceProvider,
            IServiceTargetFinder serviceTargetFinder
            ) {
        _scriptEngineName = scriptEngineName;
        _scriptSourceProvider = scriptSourceProvider;
        _serviceTargetFinder = serviceTargetFinder;
        
        _scriptEngineManager = new ScriptEngineManager();
        loadDefaultGlobalVars();
        
        _scriptSourceProvider.addWatcher(_scriptSourceWatcher);
    }

    @Override
    public Object dispatch(
            RequestWrapper request, ResponseWrapper response
            ) throws ScriptException, NoSuchMethodException {
        ServiceTarget target = _serviceTargetFinder.findOut(request);
        if(logger.isDebugEnabled()) {
            logger.debug("Script service dispatch -> "
                    + " app:" + target.app 
                    + " service:" + target.serviceName
                    + " method:" + target.methodName
                    );
        }
        CompiledScript compiledScript = getScriptSourceManager(target.app).getCompiledScript(target.serviceName);
        if(compiledScript == null) {
            throw new IllegalArgumentException(
                    "Script service target not found."
                    + " app:" + target.app 
                    + " service:" + target.serviceName
                    + " method:" + target.methodName
                    );
        }
        
        Object serviceObj = compiledScript.eval();
        return ((Invocable) compiledScript.getEngine()).invokeMethod(
                serviceObj, target.methodName, 
                parseRequestParams(request),
                request, response
                );
    }
    
    @Override
    public CompiledScript getCompiledScript(ServiceTarget target) {
        return getScriptSourceManager(target.app).getCompiledScript(target.serviceName);
    }
    
    private Map<String, String> parseRequestParams(RequestWrapper request) {
        Enumeration<String> nameEnum = request.getParameterNames();
        
        Map<String, String> paramMap = new HashMap<>();
        while(nameEnum.hasMoreElements()) {
            final String name = nameEnum.nextElement();
            final String value = request.getParameter(name);
            
            paramMap.put(name, value);
        }
        
        return paramMap;
    }
    
    private void loadDefaultGlobalVars() {
        Charset charset = Charset.forName("utf-8");        
        for(String resPath : Resource_scripts_ForDefaultGlobalVars) {
            try {
                InputStreamReader reader = new InputStreamReader(ScriptServiceDispatcher.class.getResourceAsStream(resPath), charset);
                try {
                    updateScriptSource(null, reader, null);
                } finally {
                    reader.close();
                }
            } catch(Throwable e) {
                logger.error("Error in loadDefaultGlobalVars! res:" + resPath, e);
            }
        }
    }
    
    private String updateScriptSource(String app, Reader script, String config) throws ScriptException, NoSuchMethodException {
        final ScriptEngine engine;
        final boolean isGlobal;
        if(isAppEmpty(app)) {
            engine = createScriptEngine(_scriptEngineManager);
            isGlobal = true;
        } else {
            engine = getScriptSourceManager(app).getEngine();
            isGlobal = false;
        }
        
        final CompiledScript compiledScript = ((Compilable) engine).compile(script);
        final Object jsObj = compiledScript.eval();
        if(jsObj == null) {
            return null;
        }
        
        final String serviceName;
        if(scriptObjContainsMethodServiceName(jsObj)) {
            Object retVal = ((Invocable) engine).invokeMethod(
                    jsObj, IScriptService.MethodName_serviceName
                    );
            serviceName = (String) retVal;
        } else {
            serviceName = null;
        }
        
        if(serviceName != null && serviceName.length() > 0) {
            {
                Object oldJsObj;
                if(isGlobal) {
                    oldJsObj = _scriptEngineManager.get(serviceName);
                } else {
                    oldJsObj = engine.eval(serviceName + ";");
                }
               
                if(oldJsObj != null) {
                    //destroy the old one if need
                    if(scriptObjContainsMethodDestroy(oldJsObj)) {
                        try {
                            ((Invocable) engine).invokeMethod(
                                    jsObj, IScriptContext.MethodName_destroy
                                    );
                        } catch (Throwable e) {
                            logger.error("Error occurred in ScriptObj.destroy()! app[" + app + "] serviceName:" + serviceName, e);
                        }
                    }
                }
            }
            
            //reload new one if need
            if(scriptObjContainsMethodReload(jsObj)) {
                try {
                    ((Invocable) engine).invokeMethod(
                            jsObj, IScriptContext.MethodName_reload, config
                            );
                } catch (Throwable e) {
                    throw new RuntimeException("Error occurred in ScriptObj.reload()! app[" + app + "] serviceName:" + serviceName, e);
                }
            }
            
            //put into engine or engine manager
            if(isGlobal) {
                _scriptEngineManager.put(serviceName, jsObj);
            } else {
                getScriptSourceManager(app).getEngine().put(serviceName, jsObj);
                getScriptSourceManager(app).putCompiledScript(serviceName, compiledScript);
            }
            
            return serviceName;
        } else {
            //empty serviceName means that this ScriptObject no need to put into ScriptEngine.
            //do nothing after being compiled and eval()
            return serviceName;
        }
        
    }
    
    private void deleteScriptSource(String app, String serviceName) {
        final ScriptEngine engine;
        final boolean isGlobal;
        if(isAppEmpty(app)) {
            engine = createScriptEngine(_scriptEngineManager);
            isGlobal = true;
        } else {
            engine = getScriptSourceManager(app).getEngine();
            isGlobal = false;
        }

        Object jsObj;
        if(isGlobal) {
            jsObj = _scriptEngineManager.get(serviceName);
        } else {
            jsObj = getScriptSourceManager(app).getEngine().get(serviceName);
        }
        
        if(jsObj != null) {
            //destroy the old one if need
            if(scriptObjContainsMethodDestroy(jsObj)) {
                try {
                    ((Invocable) engine).invokeMethod(
                            jsObj, IScriptContext.MethodName_destroy
                            );
                } catch (Throwable e) {
                    logger.error("Error occurred in ScriptObj.destroy()! app[" + app + "] serviceName:" + serviceName, e);
                }
            }
        }
        
        if(isGlobal) {
            _scriptEngineManager.put(serviceName, null);
        } else {
            getScriptSourceManager(app).getEngine().put(serviceName, null);
            getScriptSourceManager(app).deleteCompiledScript(serviceName);
        }
    } 
    
    private static boolean scriptObjContainsMethodReload(Object jsObj) {
        return ((Map<String, Object>)jsObj).containsKey(IScriptContext.MethodName_reload);
    }

    private static boolean scriptObjContainsMethodDestroy(Object jsObj) {
        return ((Map<String, Object>)jsObj).containsKey(IScriptContext.MethodName_destroy);
    }
    
    private static boolean scriptObjContainsMethodServiceName(Object jsObj) {
        return ((Map<String, Object>)jsObj).containsKey(IScriptService.MethodName_serviceName);
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
            
            scriptManager = new ScriptSourceManager();
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
        
        public ScriptSourceManager() {
            _engine = createScriptEngine(_scriptEngineManager);
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
    }
    
    
    private ScriptEngine createScriptEngine(ScriptEngineManager engineManager) {
        return engineManager.getEngineByName(_scriptEngineName);
    }
    
    
}
