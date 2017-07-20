package com.salama.service.script.dispatcher;

import java.io.IOException;
import java.io.Reader;
import java.lang.reflect.InvocationTargetException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import javax.script.CompiledScript;
import javax.script.Invocable;
import javax.script.ScriptException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.alibaba.fastjson.JSON;
import com.salama.service.core.net.RequestWrapper;
import com.salama.service.core.net.ResponseWrapper;
import com.salama.service.script.core.IConfigLocationResolver;
import com.salama.service.script.core.IScriptContext;
import com.salama.service.script.core.IScriptServiceDispatcher;
import com.salama.service.script.core.IScriptServicePostFilter;
import com.salama.service.script.core.IScriptServicePreFilter;
import com.salama.service.script.core.IScriptSourceContainer;
import com.salama.service.script.core.IScriptSourceProvider;
import com.salama.service.script.core.IServiceTargetFinder;
import com.salama.service.script.core.ScriptContextSetting;
import com.salama.service.script.core.ServiceTarget;
import com.salama.service.script.sourcecontainer.ScriptSourceContainer;

import MetoXML.XmlDeserializer;
import MetoXML.Base.XmlParseException;

public class ScriptServiceDispatcher implements IScriptServiceDispatcher<RequestWrapper, ResponseWrapper> {
    private final static Log logger = LogFactory.getLog(ScriptServiceDispatcher.class);
    
    //private final String _scriptEngineName;
    private ScriptServiceDispatcherConfig _config;
    private IConfigLocationResolver _configLocationResolver;
    
    
    private IScriptSourceProvider _scriptSourceProvider;
    private IServiceTargetFinder<RequestWrapper> _serviceTargetFinder;
    private IScriptSourceContainer _scriptSourceContainer;
    
    
    @Override
    public String serviceName() {
        return null;
    }
    
    @Override
    public IServiceTargetFinder<RequestWrapper> getServiceTargetFinder() {
        return _serviceTargetFinder;
    }
    
    @Override
    public IScriptSourceProvider getScriptSourceProvider() {
        return _scriptSourceProvider;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void reload(Reader config, IConfigLocationResolver configLocationResolver) throws IOException {
        try {
            _config = (ScriptServiceDispatcherConfig) XmlDeserializer.stringToObject(
                    readConfig(config), 
                    ScriptServiceDispatcherConfig.class
                    );
            _configLocationResolver = configLocationResolver;

            //init finder
            _serviceTargetFinder = (IServiceTargetFinder<RequestWrapper>) createAndReloadScriptContext(_config.getServiceTargetFinder());
            
            //init provider
            {
                Class<IScriptContext> type = (Class<IScriptContext>) getDefaultClassLoader().loadClass(
                        _config.getScriptSourceProvider().getClassName()
                        );
                _scriptSourceProvider = (IScriptSourceProvider) type.newInstance();

                //init source container
                _scriptSourceContainer = new ScriptSourceContainer();
                _scriptSourceContainer.init(_config.getScriptEngineName(), _serviceTargetFinder, configLocationResolver);
                
                //add watcher
                _scriptSourceProvider.addWatcher(_scriptSourceContainer);
                
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
        //destroy 
        try {
            _scriptSourceContainer.close();
        } catch(Throwable e) {
            logger.error(null, e);
        }
        
        try {
            _scriptSourceProvider.destroy();
        } catch(Throwable e) {
            logger.error(null, e);
        }
        
        logger.info("scrip dispatcher destroy finished ------");
    }


    @Override
    public Object dispatch(
            RequestWrapper request, ResponseWrapper response
            ) throws ScriptException, NoSuchMethodException {
        final ServiceTarget target = _serviceTargetFinder.findOut(request);
        final CompiledScript compiledScript = _scriptSourceContainer.findCompiledScript(target);
        if(compiledScript == null) {
            throw new IllegalArgumentException(
                    "Script service target not found."
                    + " app:" + target.app 
                    + " service:" + target.service
                    + " method:" + target.method
                    );
        }
        
        //parse params
        Map<String, Object> params = parseRequestParams(request);
        if(logger.isDebugEnabled()) {
            logger.debug("Script service dispatch -> "
                    + " app:" + target.app 
                    + " service:" + target.service
                    + " method:" + target.method
                    + " params:" + JSON.toJSONString(params)
                    );
        }
        
        //prefilter
        final IScriptServicePreFilter prefilter = _scriptSourceContainer.getPreFilter(target);
        if(prefilter != null) {
            Map<String, Object> filterResult = prefilter.doPreFilter(target, params, request, response);
            if(filterResult != null 
                    && (Boolean)filterResult.get(IScriptServicePreFilter.PreFilterResultKeys.override) == true
                    ) {
                return filterResult.get(IScriptServicePreFilter.PreFilterResultKeys.result);
            }
        }
        
        //invoke service
        final Object serviceObj = compiledScript.eval();
        final Object result = ((Invocable) compiledScript.getEngine()).invokeMethod(
                serviceObj, target.method, 
                params,
                request, response
                );
        
        //postfilter
        final IScriptServicePostFilter postfilter = _scriptSourceContainer.getPostFilter(target);
        if(postfilter != null) {
            Object filterResult = postfilter.doPostFilter(target, result, request, response);
            return filterResult;
        }
        
        return result;
    }
        
    private Map<String, Object> parseRequestParams(RequestWrapper request) {
        Enumeration<String> nameEnum = request.getParameterNames();
        
        Map<String, Object> paramMap = new HashMap<>();
        while(nameEnum.hasMoreElements()) {
            final String name = nameEnum.nextElement();
            final Object value = request.getParameter(name);
            
            paramMap.put(name, value);
        }
        
        return paramMap;
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
    
    
}
