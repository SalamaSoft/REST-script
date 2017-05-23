package com.salama.service.script.sourceprovider;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.log4j.Logger;

public class DirWatcher {
    private final static Logger logger = Logger.getLogger(DirWatcher.class);
    
    public static interface IWatchEventHandler {
        
        void handleEvent(WatchEvent<?> event, File file);
    } 
    
    //key:absolute path     value:WatchService
    private final List<WatchServiceEntry> _watchServiceEntryList = new ArrayList<>();
    
    public void pollEvent(IWatchEventHandler handler) {
        for(WatchServiceEntry watchEntry : _watchServiceEntryList) {
            watchEntry.pollEvent(handler);
        }
    }
    
    public void addDirToWatch(File dir) throws IOException {
        final WatchServiceEntry watchServiceEntry = new WatchServiceEntry(dir.getAbsolutePath());
        _watchServiceEntryList.add(watchServiceEntry);
    }
    
    private static class WatchServiceEntry {
        private final String _path;
        private final WatchService _watchService;
        
        public WatchServiceEntry(String path) throws IOException {
            _path = path;
            _watchService = FileSystems.getDefault().newWatchService();
            
            FileSystems.getDefault().getPath(_path).register(
                    _watchService, 
                    StandardWatchEventKinds.ENTRY_CREATE,
                    StandardWatchEventKinds.ENTRY_DELETE,
                    StandardWatchEventKinds.ENTRY_MODIFY
                    );
            
        }

        public void pollEvent(IWatchEventHandler watchEventHandler) {
            while(true) {
                final WatchKey watchKey = _watchService.poll();
                if(watchKey == null) {
                    break;
                }
                
                //handle events
                for(WatchEvent<?> event : watchKey.pollEvents()) {
                    try {
                        watchEventHandler.handleEvent(
                                event, 
                                new File(_path, ((Path) event.context()).toString())
                                );
                    } catch (Throwable e) {
                        logger.error(null, e);
                    }
                }
                
                //reset key
                boolean valid = watchKey.reset();
                if(!valid) {
                    // object no longer registered
                    logger.info("WatchKey.reset[" + _path + "]: " + valid);
                }
            }
        }
        
    }
}
