package com.salama.service.script;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;

import javax.servlet.ServletContext;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.salama.service.core.context.CommonContext;
import com.salama.service.script.config.ScriptServiceServletContextConfig;
import com.salama.service.script.core.IConfigLocationResolver;
import com.salama.service.script.core.IScriptServiceDispatcher;
import com.salama.util.http.upload.FileUploadSupport;

import MetoXML.XmlDeserializer;

public class ScriptServiceServletContext implements CommonContext {
    private static final long serialVersionUID = -4613337221562368596L;
    
    private final static Log logger = LogFactory.getLog(ScriptServiceServletContext.class);
    private static final String DefaultEncoding = "utf-8";
    
    private ScriptServiceServletContextConfig _config;
    private FileUploadSupport _fileUploadSupport = null;
    private IScriptServiceDispatcher _scriptServiceDispatcher;
    
    private File _servletContextPathDir;

    public FileUploadSupport getFileUploadSupport() {
        return _fileUploadSupport;
    }
    
    public IScriptServiceDispatcher getServiceDispatcher() {
        return _scriptServiceDispatcher;
    }
    
    @SuppressWarnings("unchecked")
    @Override
    public void reload(ServletContext servletContext, String configLocation) {
        logger.info("reload() configLocation:" + configLocation);
        String configFilePath = servletContext.getRealPath(configLocation);
        
        try {
            XmlDeserializer xmlDes = new XmlDeserializer();
            _config = (ScriptServiceServletContextConfig) xmlDes.Deserialize(
                    configFilePath, 
                    ScriptServiceServletContextConfig.class, 
                    XmlDeserializer.DefaultCharset
                    );
            
            _servletContextPathDir = new File(servletContext.getRealPath("/"));
            
            //ScriptServiceDispatcher -------------------------------
            {
                Class<? extends IScriptServiceDispatcher> serviceDispatcherClass =
                        (Class<? extends IScriptServiceDispatcher>) getDefaultClassLoader().loadClass(
                                _config.getServiceDispatcherSetting().getClassName()
                                );
                _scriptServiceDispatcher = serviceDispatcherClass.newInstance();
                final Reader dispatcherConfigReader = new InputStreamReader(
                        new FileInputStream(getRealPathFile(_config.getServiceDispatcherSetting().getConfigLocation())), 
                        DefaultEncoding
                        );
                try {
                    _scriptServiceDispatcher.reload(
                            dispatcherConfigReader, 
                            new IConfigLocationResolver() {
                                
                                @Override
                                public Reader resolveConfigLocation(String configLocation) throws IOException {
                                    if(configLocation == null || configLocation.trim().length() == 0) {
                                        return null;
                                    }
                                    
                                    return new InputStreamReader(
                                            new FileInputStream(getRealPathFile(configLocation)), 
                                            DefaultEncoding
                                            );
                                }
                            }
                            );
                } finally {
                    dispatcherConfigReader.close();
                }
                logger.info(
                        "reload()"
                        + " _scriptServiceDispatcher inited ->"
                        + " className: " + _config.getServiceDispatcherSetting().getClassName()
                        + " configLocation: " + _config.getServiceDispatcherSetting().getConfigLocation()
                        );
            }
             
            //Init FileUploadSupport -------------------------------
            {
                _fileUploadSupport = new FileUploadSupport(servletContext, 
                        DefaultEncoding, 
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
        _scriptServiceDispatcher.destroy();
    }

    private File getRealPathFile(String virtualPath) {
        if(virtualPath.charAt(0) == '/') {
            virtualPath = virtualPath.substring(1);
        }
        
        return new File(_servletContextPathDir, virtualPath);
    }
    
    private static ClassLoader getDefaultClassLoader() {
        ClassLoader classLoader = null;

        try {
            classLoader = Thread.currentThread().getContextClassLoader();
        } catch (Throwable e) {
            logger.warn("Error in currentThread.getContextClassLoader()", e);
        }
        
        if (classLoader == null) {
            classLoader = ScriptServiceServletContext.class.getClassLoader();
        }
        
        return classLoader;
    }
    
}
