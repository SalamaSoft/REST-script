package com.salama.service.script.sourcecontainer;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

import javax.script.Compilable;
import javax.script.CompiledScript;
import javax.script.Invocable;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.salama.service.script.core.IConfigLocationResolver;
import com.salama.service.script.core.IScriptContext;
import com.salama.service.script.core.IScriptService;
import com.salama.service.script.core.IScriptServicePostFilter;
import com.salama.service.script.core.IScriptServicePreFilter;
import com.salama.service.script.core.IScriptSourceContainer;
import com.salama.service.script.core.IServiceNameVerifier;
import com.salama.service.script.core.ITextFile;
import com.salama.service.script.core.ServiceTarget;

public class ScriptSourceContainer implements IScriptSourceContainer {
    private final static Log logger = LogFactory.getLog(ScriptSourceContainer.class);
    
    static {
        logger.info("ScriptSourceContainer VERSION: 1.0.1(20170810)");
    }
    
    private final static String[] Resource_scripts_ForDefaultGlobalVars = new String[] {
            "/com/salama/service/script/util/json.js", 
            "/com/salama/service/script/util/xml.js",
    };
    
    private final static String Script_GetApp = ""
            + "function $getApp() {\n"
            + "    return '$app';\n"
            + "}\n";
    
    private String _scriptEngineName;
    private IServiceNameVerifier _serviceNameVerifier;
    private IConfigLocationResolver _configLocationResolver;

    private ScriptEngineManager _scriptEngineManager;
    private ScriptEngine _defaultScriptEngine;
    //private List<String> _sortedScriptContextNameList;
    private List<ScriptObjLocation> _scriptContextLocationList;
    //key: Path of Script       value: ScriptObjLocation
    private Map<String, ScriptObjLocation> _scriptContextLocationMap;
    //key: app          value: IScriptServicePreFilter
    private Map<String, IScriptServicePreFilter> _scriptServicePreFilterMap;
    private Map<String, IScriptServicePostFilter> _scriptServicePostFilterMap;

    //key:$app    value:ScriptSourceManager
    private final ConcurrentHashMap<String, ScriptSourceManager> _scriptSourceManagerMap = new ConcurrentHashMap<String, ScriptSourceManager>();
    private final ReentrantLock _lockForScriptSourceManager = new ReentrantLock();

    private static class ScriptObjLocation {
        private final String app;
        private final String objName;
        
        public ScriptObjLocation(String app, String objName) {
            this.app = app;
            this.objName = objName;
        }
        
        @Override
        public boolean equals(Object obj) {
            ScriptObjLocation loc2 = (ScriptObjLocation) obj;
            
            if(this.app == null) {
                return (loc2.app == null && this.objName.equals(loc2.objName));
            } else {
                return (this.app.equals(loc2.app) && this.objName.equals(loc2.objName));
            }
        }
        
    }

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
        
        //_sortedScriptContextNameList = new ArrayList<>();
        _scriptContextLocationList = new ArrayList<>();
        _scriptContextLocationMap = new ConcurrentHashMap<>();
        
        _scriptServicePreFilterMap = new ConcurrentHashMap<>();
        _scriptServicePostFilterMap = new ConcurrentHashMap<>();

        //init default global vars ------
        loadDefaultGlobalVars();
    }

    @Override
    public void close() throws IOException {
        /*
        //destroy engine Objects
        try {
            for(Entry<String, ScriptSourceManager> sourceManagerEntry : _scriptSourceManagerMap.entrySet()) {
                try {
                    final String app = sourceManagerEntry.getKey();
                    
                    for(int i = sourceManagerEntry.getValue().getSortedScriptContextNameList().size() - 1; i >= 0; i--) {
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
        */
        
        for(int i = _scriptContextLocationList.size() - 1; i >= 0; i--) {
            ScriptObjLocation loc = _scriptContextLocationList.get(i);
            
            try {
                IScriptContext scriptCtx = getScriptContextByLocation(loc);
                if(scriptCtx != null) {
                    scriptCtx.destroy();
                }
            } catch(Throwable e) {
                logger.error(null, e);
            } 
        }
    }

    @Override
    public CompiledScript findCompiledScript(ServiceTarget target) {
        return getScriptSourceManager(target.app).getCompiledScript(target.service);
    }
    
    @Override
    public IScriptServicePreFilter getPreFilter(ServiceTarget target) {
        return _scriptServicePreFilterMap.get(target.app);
    }
    
    @Override
    public IScriptServicePostFilter getPostFilter(ServiceTarget target) {
        return _scriptServicePostFilterMap.get(target.app);
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
        List<ScriptCompileResult> compileResultList = new ArrayList<>();
        
        //compile only
        for(InitLoadScriptEntry entry : initLoadEntries) {
            try {
                ScriptCompileResult compileResult = compileScriptSource(
                        entry.getApp(),  entry.getScript(), entry.getConfig()
                        );
                compileResultList.add(compileResult);
                
                logger.info(
                        "script source compiled."
                        + " path:" + entry.getScript().getPath() 
                        + " app[" + entry.getApp() + "] serviceName:[" + compileResult.serviceName + "]"
                        + " compiledScript:" + compileResult.compiledScript
                        + " jsObj:" + compileResult.jsObj
                        );
            } catch (Throwable e) {
                logger.error(null, e);
            }
        }
        
        //reload only
        for(ScriptCompileResult compileResult : compileResultList) {
            try {
                reloadScriptContext(compileResult);
            } catch (Throwable e) {
                logger.error(null, e);
            }
        }
    }
    
    @Override
    public String onScriptSourceUpdated(String app, ITextFile script, Reader config) throws ScriptException {
        try {
            String serviceName = updateScriptSource(app, script, config);
            logger.info("onScriptSourceUpdated() -> app:" + app + " serviceName:" + serviceName);
            return serviceName;
        } catch (NoSuchMethodException | IOException e) {
            throw new RuntimeException("Error in updateScriptSource!", e);
        }
    }

    @Override
    public void onScriptSourceDeleted(String app, String serviceName) {
        deleteScriptSource(app, serviceName);
        logger.info("onScriptSourceDeleted() -> app:" + app + " serviceName:" + serviceName);
    }
    
    private boolean addScriptContextLocation(String app, String objName) {
        ScriptObjLocation loc = new ScriptObjLocation(app, objName);
        
        if(_scriptContextLocationList.contains(loc)) {
            return false;
        }
        
        _scriptContextLocationList.add(loc);
        return true;
    }
    
    private IScriptContext getScriptContextByLocation(ScriptObjLocation loc) {
        Object obj;
        ScriptEngine engine;
        if(loc.app == null || loc.app.trim().length() == 0) {
            obj = _scriptEngineManager.get(loc.objName);
            engine = _defaultScriptEngine;
        } else {
            engine = getScriptSourceManager(loc.app).getEngine();
            obj = engine.get(loc.objName);
        }
        if(obj == null) {
            return null;
        }
        
        if(IScriptContext.class.isAssignableFrom(obj.getClass())) {
            return (IScriptContext) obj;
        } else {
            IScriptContext scriptContext = null;
            try {
                scriptContext = jsObjToInterface(
                        (Invocable)engine, 
                        obj, 
                        IScriptContext.class
                        );
                return scriptContext;
            } catch (Throwable e) {
                logger.info(
                        "getScriptContextByLocation() "
                        + " app[" + loc.app + "] serviceName[" + loc.objName + "]"
                        + " is not a IScriptContext",
                        e
                        );
                return null;
            }
        }
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
                
                /*
                //add into contextOrderList when updating at 1st time
                if(!destroyInvoked) {
                    _sortedScriptContextNameList.add(varName);
                }
                */
            } else {
                getScriptSourceManager(app).getEngine().put(varName, obj);
                
                /*
                //add into contextOrderList when updating at 1st time
                if(!destroyInvoked) {
                    getScriptSourceManager(app).getSortedScriptContextNameList().add(varName);
                }
                */
            }

            if(IScriptContext.class.isAssignableFrom(obj.getClass())) {
                addScriptContextLocation(app, varName);
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
    
    private static class ScriptCompileResult {
        final String app;
        final Reader config;
        
        final ScriptEngine engine;
        final boolean isGlobal;
        
        final CompiledScript compiledScript;
        final Object jsObj; 
        final String serviceName;
        
        public ScriptCompileResult(
                String app, Reader config, ScriptEngine engine, boolean isGlobal,
                CompiledScript compiledScript, Object jsObj,
                String serviceName
                ) {
            this.app = app;
            this.config = config;
            this.engine = engine;
            this.isGlobal = isGlobal;
            this.compiledScript = compiledScript;
            this.jsObj = jsObj;
            this.serviceName = serviceName;
        }
    }
    
    private ScriptCompileResult compileScriptSource(
            String app, ITextFile script, Reader config
            ) throws ScriptException, IOException {
        //destroy the ScriptContext before recompile
        if(_scriptContextLocationMap.get(script.getPath()) != null) {
            IScriptContext scriptContex = getScriptContextByLocation(
                    _scriptContextLocationMap.get(script.getPath())
                    );
            if(scriptContex != null) {
                try {
                    scriptContex.destroy();
                } catch (Throwable e) {
                    logger.error(null, e);
                }
            }
        }
        
        final ScriptEngine engine;
        final boolean isGlobal;
        if(isAppEmpty(app)) {
            engine = _defaultScriptEngine;
            isGlobal = true;
        } else {
            if(!_serviceNameVerifier.verifyFormatOfApp(app)) {
                throw new RuntimeException("Invalid format of app:" + app + " path:" + script.getPath());
            }
            
            engine = getScriptSourceManager(app).getEngine();
            isGlobal = false;
        }
        
        final String scriptStr = readTextFile(script);
        final CompiledScript compiledScript;
        final Object jsObj;
        try {
            compiledScript = ((Compilable) engine).compile(scriptStr);
            jsObj = compiledScript.eval();
        } catch (ScriptException e) {
            logger.error("Error occurred in compiling script. path:" + script.getPath(), e);
            throw e;
        }
        
        String serviceName = null;
        
        if(jsObj != null) {
            final IScriptService scriptService = jsObjToInterface((Invocable) engine, jsObj, IScriptService.class);
            if(scriptService == null) {
                serviceName = null;
            } else {
                serviceName = scriptService.serviceName();
            }
            
            if(serviceName != null && serviceName.length() > 0) {
                if(!_serviceNameVerifier.verifyFormatOfServiceName(serviceName)) {
                    throw new RuntimeException("Invalid format of serviceName:" + serviceName + " path:" + script.getPath());
                }
                
                final IScriptContext scriptContext = jsObjToInterface((Invocable) engine, jsObj, IScriptContext.class);
                if(scriptContext == null) {
                    //Not ScriptContext
                    if(isGlobal) {
                        _scriptEngineManager.put(serviceName, jsObj);
                    } else {
                        //stored as a service
                        getScriptSourceManager(app).putCompiledScript(serviceName, compiledScript);
                    }
                } else {
                    _scriptContextLocationMap.put(script.getPath(), new ScriptObjLocation(app, serviceName));
                }
            }
        }
        
        return new ScriptCompileResult(
                app, config, engine, isGlobal, 
                compiledScript, jsObj,
                serviceName
                );
    }
    
    private boolean reloadScriptContext(final ScriptCompileResult compileResult) {
        if(compileResult.jsObj == null) {
            return false;
        }
        if(compileResult.serviceName == null || compileResult.serviceName.length() == 0) {
            return false;
        }
        
        //destroyed before recompile
        //boolean destroyInvoked = destroyWhenScriptContext(compileResult.app, compileResult.serviceName);

        //store the jsObj
        final IScriptContext scriptContext = jsObjToInterface((Invocable) compileResult.engine, compileResult.jsObj, IScriptContext.class);
        if(scriptContext != null) {
            //A ScriptContext will not be exposed as a service
            //reload
            try {
                scriptContext.reload(compileResult.config, _configLocationResolver);
                logger.info("ScriptContext reloaded ->"
                        + " app[" + compileResult.app + "] serviceName:[" + compileResult.serviceName + "]" 
                        );
            } catch (Throwable e) {
                logger.error(
                        "Error occurred in reloading ScriptContext! ->"
                        + " app[" + compileResult.app + "] serviceName:[" + compileResult.serviceName + "]", 
                        e
                        );
            }
            
            //stored
            if(compileResult.isGlobal) {
                _scriptEngineManager.put(compileResult.serviceName, compileResult.jsObj);
                
                /*
                //add into contextOrderList when updating at 1st time
                if(!destroyInvoked) {
                    _sortedScriptContextNameList.add(compileResult.serviceName);
                }
                */
            } else {
                getScriptSourceManager(compileResult.app).getEngine().put(compileResult.serviceName, compileResult.jsObj);
                
                /*
                //add into contextOrderList when updating at 1st time
                if(!destroyInvoked) {
                    getScriptSourceManager(compileResult.app).getSortedScriptContextNameList().add(compileResult.serviceName);
                }
                */
                
                //filters ------
                {
                    final IScriptServicePreFilter servicePreFilter = jsObjToInterface(
                            (Invocable) compileResult.engine, compileResult.jsObj, 
                            IScriptServicePreFilter.class
                            );
                    if(servicePreFilter != null) {
                        _scriptServicePreFilterMap.put(compileResult.app, servicePreFilter);
                        logger.info("servicePreFilter loaded. app:" + compileResult.app + " serviceName:" + servicePreFilter.serviceName());
                    }
                }
                {
                    final IScriptServicePostFilter servicePostFilter = jsObjToInterface(
                            (Invocable) compileResult.engine, compileResult.jsObj, 
                            IScriptServicePostFilter.class
                            );
                    if(servicePostFilter != null) {
                        _scriptServicePostFilterMap.put(compileResult.app, servicePostFilter);
                        logger.info("servicePostFilter loaded. app:" + compileResult.app + " serviceName:" + servicePostFilter.serviceName());
                    }
                }
            }
            addScriptContextLocation(compileResult.app, compileResult.serviceName);
            
            return true;
        } else {
            
            return false;
        }
    }
    private String updateScriptSource(
            String app, ITextFile script, Reader config
            ) throws ScriptException, NoSuchMethodException, IOException {
        final ScriptCompileResult compileResult = compileScriptSource(app, script, config);
        logger.info(
                "script source compiled."
                + " path:" + script.getPath()
                + " app[" + app + "] serviceName:[" + compileResult.serviceName + "]"
                + " compiledScript:" + compileResult.compiledScript
                + " jsObj:" + compileResult.jsObj
                );
        
        if(compileResult.jsObj == null) {
            logger.info(
                    "script source updated. (null returned by eval())"
                    + " path:" + script.getPath()
                    + " app[" + app + "] serviceName:[null]"
                    + " compiledScript:" + compileResult.compiledScript
                    );
            return null;
        }
        
        if(compileResult.serviceName == null || compileResult.serviceName.length() == 0) {
            logger.warn(
                    "script source updated."
                    + " path:" + script.getPath()
                    + " app[" + app + "] serviceName:[" + compileResult.serviceName + "]"
                    + " compiledScript:" + compileResult.compiledScript
                    );
            return null;
        }
        
        reloadScriptContext(compileResult);

        logger.info(
                "script source updated."
                + " path:" + script.getPath()
                + " app[" + app + "] serviceName:[" + compileResult.serviceName + "]"
                + " compiledScript:" + compileResult.compiledScript
                );
        return compileResult.serviceName;
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
    
    protected static String readTextFile(ITextFile textFile) throws IOException {
        try(final Reader reader = textFile.getReader();) {
            return readAsString(reader);
        }
    }
    protected static String readAsString(Reader reader) throws IOException {
        StringBuilder str = new StringBuilder();
        char[] cBuf = new char[512];

        int bufLen = cBuf.length;
        int readLen;
        while(true) {
            readLen = reader.read(cBuf, 0, bufLen);

            if(readLen < 0) {
                break;
            }

            if(readLen > 0) {
                str.append(cBuf, 0, readLen);
            }
        }

        return str.toString();
    }
    
    protected static Reader readerWrapString(String str) {
        return new StringReader(str);
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
        
        //private final List<String> _sortedScriptContextNameList = new ArrayList<>();        
        
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
        
        /*
        public void addScriptContextName(String ctxName) {
            this._sortedScriptContextNameList.add(ctxName);
        }
        
        public List<String> getSortedScriptContextNameList() {
            return _sortedScriptContextNameList;
        }
        */
    }
    
    
    private ScriptEngine createScriptEngine(ScriptEngineManager engineManager) {
        return engineManager.getEngineByName(_scriptEngineName);
    }

}
