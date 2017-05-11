package com.salama.service.script;

import javax.servlet.ServletContext;

import org.apache.log4j.Logger;

import com.salama.service.core.context.CommonContext;
import com.salama.service.script.config.ScriptContextSetting;
import com.salama.service.script.config.ScriptServletConfig;
import com.salama.service.script.core.IScriptSourceProvider;
import com.salama.service.script.core.IServiceTargetFinder;
import com.salama.util.ClassLoaderUtil;

import MetoXML.XmlDeserializer;
import MetoXML.Util.ClassFinder;

class ScriptServiceContext implements CommonContext {
    private static final long serialVersionUID = -4613337221562368596L;
    
    private final static Logger logger = Logger.getLogger(ScriptServiceContext.class);
    
    private ScriptServletConfig _config;
    private IScriptSourceProvider _scriptSourceProvider;
    private ScriptServiceDispatcher _scriptServiceDispatcher;
    private IServiceTargetFinder _serviceTargetFinder;

    public ScriptServiceDispatcher getServiceDispatcher() {
        return _scriptServiceDispatcher;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void reload(ServletContext servletContext, String configLocation) {
        logger.info("reload() configLocation:" + configLocation);
        String configFilePath = servletContext.getRealPath(configLocation);

        try {
            XmlDeserializer xmlDes = new XmlDeserializer();
            _config = (ScriptServletConfig) xmlDes.Deserialize(
                    configFilePath, 
                    ScriptServletConfig.class, 
                    XmlDeserializer.DefaultCharset,
                    new ClassFinder() {
                        
                        @Override
                        public Class<?> findClass(String className) throws ClassNotFoundException {
                            if(className.equalsIgnoreCase(ScriptContextSetting.class.getSimpleName())) {
                                return ScriptContextSetting.class;
                            } else {
                                return null;
                            }
                        }
                    });
            
            //ScriptSourceProvider -------------------------------
            Class<? extends IScriptSourceProvider> typeScriptSourceProvider = 
                    (Class<? extends IScriptSourceProvider>) ClassLoaderUtil.getDefaultClassLoader().loadClass(
                            _config.getScriptSourceProviderSetting().getClassName()
                            );
            _scriptSourceProvider = typeScriptSourceProvider.newInstance();
            _scriptSourceProvider.reload(servletContext, _config.getScriptSourceProviderSetting().getConfigLocation());
            logger.info(
                    "reload()"
                    + " ScriptSourceProvider loaded -> " + _config.getScriptSourceProviderSetting().getClassName()
                    + " configLocation:" + _config.getScriptSourceProviderSetting().getConfigLocation()
                    );
            
            //IServiceTargetFinder -------------------------------
            String serviceTargetFinderClass = _config.getServiceTargetFinder();
            if(serviceTargetFinderClass != null && serviceTargetFinderClass.trim().length() > 0) {
                Class<? extends IServiceTargetFinder> cls = 
                        (Class<? extends IServiceTargetFinder>) ClassLoaderUtil.getDefaultClassLoader().loadClass(
                                serviceTargetFinderClass
                                );
                _serviceTargetFinder = cls.newInstance();
            } else {
                _serviceTargetFinder = new DefaultServiceTargetFinder();
            }
            logger.info(
                    "reload()"
                    + " _serviceTargetFinder inited -> " + _serviceTargetFinder.getClass().getName()
                    );
            
            //ScriptServiceDispatcher -------------------------------
            _scriptServiceDispatcher = new ScriptServiceDispatcher(
                    _config.getScriptEngineName(), 
                    _scriptSourceProvider,
                    _serviceTargetFinder
                    );
            logger.info(
                    "reload()"
                    + " _scriptServiceDispatcher inited -> engineName: " + _config.getScriptEngineName()
                    );
            
             
            logger.info("reload() finished -----");
        } catch (Throwable e) {
            logger.error("reload()", e);
            return;
        }
    }

    @Override
    public void destroy() {
        _scriptSourceProvider.destroy();
    }

}
