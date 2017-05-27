package com.salama.service.script.dispatcher;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.reflect.InvocationTargetException;
import java.nio.charset.Charset;
import java.text.MessageFormat;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
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

import com.salama.service.core.net.RequestWrapper;
import com.salama.service.core.net.ResponseWrapper;
import com.salama.service.script.core.IConfigLocationResolver;
import com.salama.service.script.core.IScriptContext;
import com.salama.service.script.core.IScriptService;
import com.salama.service.script.core.IScriptServiceDispatcher;
import com.salama.service.script.core.IScriptSourceProvider;
import com.salama.service.script.core.IScriptSourceWatcher;
import com.salama.service.script.core.IServiceTargetFinder;
import com.salama.service.script.core.ScriptContextSetting;
import com.salama.service.script.core.ServiceTarget;

import MetoXML.XmlDeserializer;
import MetoXML.Base.XmlParseException;

public class ScriptServiceDispatcher implements IScriptServiceDispatcher {
    private final static Logger logger = Logger.getLogger(ScriptServiceDispatcher.class);
    
    public final static String[] Resource_scripts_ForDefaultGlobalVars = new String[] {
            "/com/salama/service/script/resource/script/json.js", 
            "/com/salama/service/script/resource/script/xml.js",
    };
    
    public final static String Script_GetApp = ""
            + "function $getApp() {\n"
            + "    return '$app';\n"
            + "}\n";
    
    //private final String _scriptEngineName;
    private ScriptServiceDispatcherConfig _config;
    private IConfigLocationResolver _configLocationResolver;
    
    private ScriptEngineManager _scriptEngineManager;
    private ScriptEngine _defaultScriptEngine;
    
    private IScriptSourceProvider _scriptSourceProvider;
    private IServiceTargetFinder _serviceTargetFinder;
    private IScriptSourceWatcher _scriptSourceWatcher;    
    
    //key:$app    value:ScriptSourceManager
    private final ConcurrentHashMap<String, ScriptSourceManager> _scriptSourceManagerMap = new ConcurrentHashMap<String, ScriptSourceManager>();
    private final ReentrantLock _lockForScriptSourceManager = new ReentrantLock();
    
    @Override
    public String serviceName() {
        return null;
    }
    
    @Override
    public IScriptSourceWatcher getScriptSourceWatcher() {
        return _scriptSourceWatcher;
    }
    
    @Override
    public IServiceTargetFinder getServiceTargetFinder() {
        return _serviceTargetFinder;
    }

    @Override
    public void reload(Reader config, IConfigLocationResolver configLocationResolver) throws IOException {
        try {
            _config = (ScriptServiceDispatcherConfig) XmlDeserializer.stringToObject(
                    readConfig(config), 
                    ScriptServiceDispatcherConfig.class
                    );
            _configLocationResolver = configLocationResolver;

            _scriptEngineManager = new ScriptEngineManager();
            _defaultScriptEngine = createScriptEngine(_scriptEngineManager);

            //init default global vars ------
            loadDefaultGlobalVars();
            
            //init finder
            _serviceTargetFinder = (IServiceTargetFinder) createAndReloadScriptContext(_config.getServiceTargetFinder());
            
            //init provider
            {
                @SuppressWarnings("unchecked")
                Class<IScriptContext> type = (Class<IScriptContext>) getDefaultClassLoader().loadClass(
                        _config.getScriptSourceProvider().getClassName()
                        );
                _scriptSourceProvider = (IScriptSourceProvider) type.newInstance();
                
                //add watcher 
                _scriptSourceWatcher = new MyScriptSourceWatcher();
                _scriptSourceProvider.addWatcher(_scriptSourceWatcher);
                
                //reload
                Reader configReader = _configLocationResolver.resolveConfigLocation(_config.getScriptSourceProvider().getConfigLocation());
                try {
                    _scriptSourceProvider.reload(
                            configReader, 
                            _configLocationResolver
                            );
                } finally {
                    configReader.close();
                }
                
            }
            
        } catch (InvocationTargetException | IllegalAccessException | InstantiationException | NoSuchMethodException
                | XmlParseException | ClassNotFoundException
                e ) {
            throw new IOException(e);
        }
    }

    @Override
    public void destroy() {
        //destroy engine Objects
        try {
            for(Entry<String, ScriptSourceManager> sourceManagerEntry : _scriptSourceManagerMap.entrySet()) {
                try {
                    final String app = sourceManagerEntry.getKey();
                    
                    for(Entry<String, Object> objEntry : sourceManagerEntry.getValue()
                            .getEngine().getBindings(ScriptContext.ENGINE_SCOPE).entrySet()) {
                        final String serviceName = objEntry.getKey();
                        final Object obj = objEntry.getValue();
                        
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
        for(Entry<String, Object> objEntry : _scriptEngineManager.getBindings().entrySet()) {
            try {
                final String varName = objEntry.getKey();
                final Object obj = objEntry.getValue();
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
        
        //destroy 
        try {
            _scriptSourceProvider.destroy();
        } catch(Throwable e) {
            logger.error(null, e);
        }
        
        logger.info("scrip dispatcher destroy finished ------");
    }

    private class MyScriptSourceWatcher implements IScriptSourceWatcher {
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
    public CompiledScript findCompiledScript(ServiceTarget target) {
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
    
    @SuppressWarnings("unchecked")
    private IScriptContext createAndReloadScriptContext(ScriptContextSetting setting) throws InstantiationException, IllegalAccessException, ClassNotFoundException, IOException {
        Class<IScriptContext> type = (Class<IScriptContext>) getDefaultClassLoader().loadClass(setting.getClassName());
        IScriptContext scriptContext = (IScriptContext) type.newInstance();
        Reader configReader = _configLocationResolver.resolveConfigLocation(setting.getConfigLocation()); 
        try {
            scriptContext.reload(configReader, _configLocationResolver);
        } finally {
            if(configReader != null) {
                configReader.close();
            }
        }
        
        return scriptContext;
    }
    
    private boolean updateJavaObj(String app, String varName, Object obj, Reader config) {
        final boolean isGlobal;
        try {
            final Object oldObj;
            if(isAppEmpty(app)) {
                isGlobal = true;
                oldObj = _scriptEngineManager.get(varName);
            } else {
                if(!_serviceTargetFinder.verifyFormatOfApp(app)) {
                    throw new RuntimeException("Invalid format of app:" + app);
                }
                
                isGlobal = false;
                oldObj = getScriptSourceManager(app).getEngine().get(varName);
            }
            
            if(oldObj != null) {
                try {
                    if(destroyJavaObj(oldObj)) {
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
            } else {
                getScriptSourceManager(app).getEngine().put(varName, obj);
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
            if(!_serviceTargetFinder.verifyFormatOfApp(app)) {
                throw new RuntimeException("Invalid format of app:" + app);
            }
            
            engine = getScriptSourceManager(app).getEngine();
            isGlobal = false;
        }
        
        final CompiledScript compiledScript = ((Compilable) engine).compile(script);
        final Object jsObj = compiledScript.eval();
        if(jsObj == null) {
            logger.info(
                    "script source updated."
                    + " app[" + app + "] serviceName:[null]"
                    + " compiledScript:" + compiledScript
                    );
            return null;
        }
        
        final IScriptService scriptService = jsObjToInterface((Invocable) engine, jsObj, IScriptService.class);
        String serviceName = scriptService.serviceName();
        if(serviceName == null || serviceName.length() == 0) {
            logger.warn(
                    "script source updated."
                    + " app[" + app + "] serviceName:[" + serviceName + "]"
                    + " compiledScript:" + compiledScript
                    );
            return null;
        }
        
        if(!_serviceTargetFinder.verifyFormatOfServiceName(serviceName)) {
            throw new RuntimeException("Invalid format of serviceName:" + serviceName);
        }
        
        //destroy old one ---------------------------------------------
        destroyWhenScriptContext(app, serviceName);

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
            } else {
                getScriptSourceManager(app).getEngine().put(serviceName, jsObj);
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
    
    private void destroyWhenScriptContext(String app, String serviceName) {
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
                }
            } catch (Throwable e) {
                logger.error(
                        "Error occurred in destroying ScriptContext! ->"
                        + " app[" + app + "] serviceName:[" + serviceName + "]", 
                        e
                        );
            }
        }
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
    
    private static ClassLoader getDefaultClassLoader() {
        ClassLoader classLoader = null;

        try {
            classLoader = Thread.currentThread().getContextClassLoader();
        } catch (Throwable e) {
            logger.warn("Error in currentThread.getContextClassLoader()", e);
        }
        
        if (classLoader == null) {
            classLoader = ScriptServiceDispatcher.class.getClassLoader();
        }
        
        return classLoader;
    }
    
    private static String readConfig(Reader config) throws IOException {
        StringBuilder str = new StringBuilder();

        char[] cBuf = new char[256];
        final int bufLen = cBuf.length;
        int readLen;
        while(true) {
            readLen = config.read(cBuf, 0, bufLen);
            
            if(readLen < 0) {
                break;
            }
            
            if(readLen > 0) {
                str.append(cBuf, 0, readLen);
            }
        }
        
        return str.toString();
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
        
    }
    
    
    private ScriptEngine createScriptEngine(ScriptEngineManager engineManager) {
        return engineManager.getEngineByName(_config.getScriptEngineName());
    }
    
    
}
