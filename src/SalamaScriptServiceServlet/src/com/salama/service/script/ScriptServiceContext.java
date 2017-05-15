package com.salama.service.script;

import java.io.File;
import java.io.FileReader;

import javax.servlet.ServletContext;

import org.apache.log4j.Logger;

import com.salama.service.core.context.CommonContext;
import com.salama.service.script.config.ScriptProviderSetting;
import com.salama.service.script.config.ScriptServiceContextConfig;
import com.salama.service.script.config.ScriptServiceDispatcherConfig;
import com.salama.service.script.config.ServletUploadSetting;
import com.salama.service.script.core.IScriptServiceDispatcher;
import com.salama.service.script.core.IScriptSourceProvider;
import com.salama.service.script.core.IServiceTargetFinder;
import com.salama.util.http.upload.FileUploadSupport;

import MetoXML.XmlDeserializer;
import MetoXML.Util.ClassFinder;

public class ScriptServiceContext implements CommonContext {
    private static final long serialVersionUID = -4613337221562368596L;
    
    private final static Logger logger = Logger.getLogger(ScriptServiceContext.class);
    private static final String DefaultEncoding = "utf-8";
    
    private ScriptServiceContextConfig _config;
    
    private FileUploadSupport _fileUploadSupport = null;
    
    private IScriptSourceProvider _scriptSourceProvider;
    private IScriptServiceDispatcher _scriptServiceDispatcher;

    public FileUploadSupport getFileUploadSupport() {
        return _fileUploadSupport;
    }
    
    public IScriptServiceDispatcher getServiceDispatcher() {
        return _scriptServiceDispatcher;
    }
    
    public String getEncoding() {
        return _config.getEncoding();
    }

    @SuppressWarnings("unchecked")
    @Override
    public void reload(ServletContext servletContext, String configLocation) {
        logger.info("reload() configLocation:" + configLocation);
        String configFilePath = servletContext.getRealPath(configLocation);

        
        try {
            XmlDeserializer xmlDes = new XmlDeserializer();
            _config = (ScriptServiceContextConfig) xmlDes.Deserialize(
                    configFilePath, 
                    ScriptServiceContextConfig.class, 
                    XmlDeserializer.DefaultCharset,
                    new ClassFinder() {
                        
                        @Override
                        public Class<?> findClass(String className) throws ClassNotFoundException {
                            if(className.equalsIgnoreCase(ScriptProviderSetting.class.getSimpleName())) {
                                return ScriptProviderSetting.class;
                            } if(className.equalsIgnoreCase(ScriptServiceDispatcherConfig.class.getSimpleName())) {
                                return ScriptServiceDispatcherConfig.class;
                            } if(className.equalsIgnoreCase(ServletUploadSetting.class.getSimpleName())) {
                                return ServletUploadSetting.class;
                            } else {
                                return null;
                            }
                        }
                    });
            
            if(_config.getEncoding() == null || _config.getEncoding().trim().length() == 0) {
                _config.setEncoding(DefaultEncoding);
            }
            
            //ScriptSourceProvider -------------------------------
            {
                Class<? extends IScriptSourceProvider> typeScriptSourceProvider = 
                        (Class<? extends IScriptSourceProvider>) getDefaultClassLoader().loadClass(
                                _config.getServiceDispatcherConfig().getScriptSourceProviderSetting().getClassName()
                                );
                _scriptSourceProvider = typeScriptSourceProvider.newInstance();
               
                FileReader reader = new FileReader(
                        new File(servletContext.getRealPath(
                                _config.getServiceDispatcherConfig().getScriptSourceProviderSetting().getConfigLocation()
                                ))
                        );
                try {
                    _scriptSourceProvider.reload(reader);
                } finally {
                    reader.close();
                }
                logger.info(
                        "reload()"
                        + " ScriptSourceProvider loaded -> " + _config.getServiceDispatcherConfig().getScriptSourceProviderSetting().getClassName()
                        + " configLocation:" + _config.getServiceDispatcherConfig().getScriptSourceProviderSetting().getConfigLocation()
                        );
            }
            
            //ScriptServiceDispatcher -------------------------------
            {
                //IServiceTargetFinder 
                IServiceTargetFinder _serviceTargetFinder;
                String serviceTargetFinderClass = _config.getServiceDispatcherConfig().getServiceTargetFinder();
                if(serviceTargetFinderClass != null && serviceTargetFinderClass.trim().length() > 0) {
                    Class<? extends IServiceTargetFinder> cls = 
                            (Class<? extends IServiceTargetFinder>) getDefaultClassLoader().loadClass(
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
                
                //ScriptServiceDispatcher
                _scriptServiceDispatcher = new ScriptServiceDispatcher(
                        _config.getServiceDispatcherConfig().getScriptEngineName(), 
                        _scriptSourceProvider,
                        _serviceTargetFinder
                        );
                logger.info(
                        "reload()"
                        + " _scriptServiceDispatcher inited -> engineName: " + _config.getServiceDispatcherConfig().getScriptEngineName()
                        );
            }
             
            //Init FileUploadSupport -------------------------------
            {
                _fileUploadSupport = new FileUploadSupport(servletContext, 
                        _config.getEncoding(), 
                        _config.getServletUploadSetting().getFileSizeMax(), 
                        _config.getServletUploadSetting().getSizeMax(), 
                        _config.getServletUploadSetting().getSizeThreshold(), 
                        _config.getServletUploadSetting().getTempDirPath()
                        );
                logger.info("_fileUploadSupport.tempDirPath:" + _fileUploadSupport.getTempDirPath());
            }
            
            logger.info("reload() finished -----");
        } catch (Throwable e) {
            logger.error("ScriptService reload - Error occurred", e);
            return;
        }
    }

    @Override
    public void destroy() {
        _scriptSourceProvider.destroy();
    }

    public static ClassLoader getDefaultClassLoader() {
        ClassLoader classLoader = null;

        try {
            classLoader = Thread.currentThread().getContextClassLoader();
        } catch (Throwable e) {
        }
        
        if (classLoader == null) {
            classLoader = ScriptServiceContext.class.getClassLoader();
        }
        
        return classLoader;
    }
    
}
