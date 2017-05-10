package com.salama.service.script;

import javax.servlet.ServletContext;

import org.apache.log4j.Logger;

import com.salama.service.core.context.CommonContext;
import com.salama.service.script.config.ScriptContextSetting;
import com.salama.service.script.config.ScriptServletConfig;
import com.salama.service.script.core.IScriptSourceProvider;
import com.salama.util.ClassLoaderUtil;

import MetoXML.XmlDeserializer;
import MetoXML.XmlSerializer;
import MetoXML.Util.ClassFinder;

class ScriptServiceContext implements CommonContext {
    private static final long serialVersionUID = -4613337221562368596L;
    
    private final static Logger logger = Logger.getLogger(ScriptServiceContext.class);
    
    private ScriptServletConfig _config;
    private IScriptSourceProvider _scriptSourceProvider;
    
    public IScriptSourceProvider getScriptSourceProvider() {
        return _scriptSourceProvider;
    }

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
            
            Class<? extends IScriptSourceProvider> typeScriptSourceProvider = 
                    (Class<? extends IScriptSourceProvider>) ClassLoaderUtil.getDefaultClassLoader().loadClass(
                            _config.getScriptSourceProviderSetting().getClassName()
                            );
            _scriptSourceProvider = typeScriptSourceProvider.newInstance();
            _scriptSourceProvider.reload(servletContext, _config.getScriptSourceProviderSetting().getConfigLocation());
            logger.info(
                    "reload()"
                    + " ScriptSourceProvider loaded:" + _config.getScriptSourceProviderSetting().getClassName()
                    + " configLocation:" + _config.getScriptSourceProviderSetting().getConfigLocation()
                    );
            
            logger.info("reload() finished");
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
