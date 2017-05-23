package com.salama.service.script.sourceprovider;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.ClosedWatchServiceException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import com.salama.service.script.core.IConfigLocationResolver;
import com.salama.service.script.core.IScriptSourceProvider;
import com.salama.service.script.core.IScriptSourceWatcher;
import com.salama.service.script.sourceprovider.DirWatcher.IWatchEventHandler;
import com.salama.service.script.sourceprovider.config.ScriptAppSetting;
import com.salama.service.script.sourceprovider.config.ScriptContextInitSetting;
import com.salama.service.script.sourceprovider.config.ScriptSourceFileProviderConfig;

import MetoXML.XmlDeserializer;
import MetoXML.Base.XmlParseException;
import MetoXML.Util.ClassFinder;

public class ScriptSourceFileProvider implements IScriptSourceProvider, IWatchEventHandler {
    private final static Logger logger = Logger.getLogger(ScriptSourceFileProvider.class);
    
    private final List<IScriptSourceWatcher> _scriptWatchers = new ArrayList<IScriptSourceWatcher>();
    
    private ScriptSourceFileProviderConfig _config;
    
    @Override
    public void addWatcher(IScriptSourceWatcher watcher) {
        _scriptWatchers.add(watcher);
    }

    @Override
    public void reload(Reader config, IConfigLocationResolver configLocationResolver) throws IOException {
        try {
            _config = (ScriptSourceFileProviderConfig) XmlDeserializer.stringToObject(
                    readText(config), 
                    ScriptSourceFileProviderConfig.class, 
                    new ClassFinder() {
                        
                        @Override
                        public Class<?> findClass(String className) throws ClassNotFoundException {
                            if(className.equals(ScriptContextInitSetting.class.getSimpleName())) {
                                return ScriptContextInitSetting.class;
                            } else if (className.equals(ScriptAppSetting.class.getSimpleName())) { 
                                return ScriptAppSetting.class;
                            } else {
                                return null;
                            }
                        }
                    }
                    );
            logger.info("ScriptSourceFileProvider reload() start ->"
                    + " sourceStorageDir:" + _config.getSourceStorageDir()
                    );
            
            FileSystem fs = FileSystems.getDefault();
            _pathWatcher = fs.newWatchService();
            
            _sourceRootPath = fs.getPath(_config.getSourceStorageDir());
            _sourceRootPath.register(
                    _pathWatcher, 
                    StandardWatchEventKinds.ENTRY_CREATE,
                    StandardWatchEventKinds.ENTRY_DELETE,
                    StandardWatchEventKinds.ENTRY_MODIFY
                    );

            //start watch thread
            _watchThread = new Thread(new Runnable() {
                
                @Override
                public void run() {
                    try {
                        while(true) {
                            WatchKey watchKey = _pathWatcher.take();
                            
                            //handle events
                            for(WatchEvent<?> event : watchKey.pollEvents()) {
                                try {
                                    handleWatchEvent(event);
                                } catch(Throwable e) {
                                    logger.error("Error occurred in handleWatchEvent()", e);
                                }
                            }
                            
                            //reset key
                            boolean valid = watchKey.reset();
                            if(!valid) {
                                // object no longer registered
                                logger.info("WatchKey.reset: " + valid);
                            }
                        }
                    } catch (InterruptedException | ClosedWatchServiceException e) {
                        logger.info("watcher event loop end.");
                    }
                }
            });
            _watchThread.start();
            
            logger.info("ScriptSourceFileProvider reload() done ->"
                    + " sourceStorageDir:" + _config.getSourceStorageDir()
                    );
        } catch (InvocationTargetException | IllegalAccessException | InstantiationException | NoSuchMethodException
                | XmlParseException 
                e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void destroy() {
        try {
            _scriptWatchers.clear();
            
            _pathWatcher.close();
            logger.info("ScriptSourceFileProvider destroy() done");
            
            //fail-safe
            _watchThread.interrupt();
        } catch (Throwable e) {
            logger.error(null, e);
        }
    }

    @Override
    public String serviceName() {
        return null;
    }

    @Override
    public void handleEvent(WatchEvent<?> event, File file) {
        if(event.kind() == StandardWatchEventKinds.ENTRY_CREATE
                || event.kind() == StandardWatchEventKinds.ENTRY_MODIFY
                ) {
            for(IScriptSourceWatcher sourceWatcher : _scriptWatchers) {
                try {
                    sourceWatcher.onScriptSourceUpdated(app, script, config)
                } catch (Throwable e) {
                    logger.error(null, e);
                }
            }
        } else if(event.kind() == StandardWatchEventKinds.ENTRY_DELETE) {
            
        } else {
            throw new RuntimeException("Unexpected event kind:" + event.kind());
        }
    }
    
    private static String readText(Reader reader) throws IOException {
        StringBuilder str = new StringBuilder();

        char[] cBuf = new char[256];
        final int bufLen = cBuf.length;
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
    

}
