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

import org.apache.log4j.Logger;

import com.salama.service.core.net.RequestWrapper;
import com.salama.service.core.net.ResponseWrapper;
import com.salama.service.script.core.IConfigLocationResolver;
import com.salama.service.script.core.IScriptContext;
import com.salama.service.script.core.IScriptServiceDispatcher;
import com.salama.service.script.core.IScriptSourceContainer;
import com.salama.service.script.core.IScriptSourceProvider;
import com.salama.service.script.core.IServiceTargetFinder;
import com.salama.service.script.core.ScriptContextSetting;
import com.salama.service.script.core.ServiceTarget;
import com.salama.service.script.sourcecontainer.ScriptSourceContainer;

import MetoXML.XmlDeserializer;
import MetoXML.Base.XmlParseException;

public class ScriptServiceDispatcher implements IScriptServiceDispatcher<RequestWrapper, ResponseWrapper> {
    private final static Logger logger = Logger.getLogger(ScriptServiceDispatcher.class);
    
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
        ServiceTarget target = _serviceTargetFinder.findOut(request);
        if(logger.isDebugEnabled()) {
            logger.debug("Script service dispatch -> "
                    + " app:" + target.app 
                    + " service:" + target.serviceName
                    + " method:" + target.methodName
                    );
        }
        CompiledScript compiledScript = _scriptSourceContainer.findCompiledScript(target);
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
