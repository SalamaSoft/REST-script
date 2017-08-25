package com.salama.service.script.sourceprovider;

import java.io.Closeable;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class DirWatcher implements Closeable {
    private final static Log logger = LogFactory.getLog(DirWatcher.class);
    
    public static interface IWatchEventHandler {
        
        void handleEvent(WatchEvent<?> event, File file);
    }
    
    private final static FileFilter _dirFilter = new FileFilter() {
        
        @Override
        public boolean accept(File file) {
            return file.isDirectory();
        }
    };
    
    //key:absolute path     value:WatchService
    //private final List<WatchServiceEntry> _watchServiceEntryList = new ArrayList<>();
    private final Map<String, WatchServiceEntry> _watchServiceEntryMap = new ConcurrentHashMap<>();
    
    private final boolean _recursively;
    
    public DirWatcher(boolean recursively) {
        _recursively = recursively;
    }
    
    public void pollEvent(IWatchEventHandler handler) {
        /*
        for(WatchServiceEntry watchEntry : _watchServiceEntryList) {
            watchEntry.pollEvent(handler);
        }
        */
        for(Entry<String, WatchServiceEntry> entry : _watchServiceEntryMap.entrySet()) {
            entry.getValue().pollEvent(handler);
        }
    }
    
    public void addDirToWatch(File dir) throws IOException {
        if(_recursively) {
            _addDirToWatchRecursively(dir);
        } else {
            _addDirToWatch(dir);
        }
    }
    
    private boolean _addDirToWatch(File dir) throws IOException {
        String path = dir.getAbsolutePath();
        if(_watchServiceEntryMap.containsKey(path)) {
            return false;
        }
        
        if(dir.isHidden()) {
            logger.debug("ignore hidden directory:" + path);
            return false;
        }
        
        final WatchServiceEntry watchServiceEntry = new WatchServiceEntry(path);
        //_watchServiceEntryList.add(watchServiceEntry);
        return _watchServiceEntryMap.put(path, watchServiceEntry) == null;
    }
    
    private void _addDirToWatchRecursively(File dir) throws IOException {
        if(!_addDirToWatch(dir)) {
            return ;
        }
        
        File[] files = dir.listFiles(_dirFilter);
        if(files != null && files.length > 0) {
            for(File subdir : files) {
                _addDirToWatchRecursively(subdir);
            }
        }
    }
    
    
    @Override
    public void close() throws IOException {
        /*
        for(WatchServiceEntry watchEntry : _watchServiceEntryList) {
            try {
                watchEntry._watchService.close();
            } catch (Throwable e) {
                logger.error(null, e);
            }
        }
        */
        for(Entry<String, WatchServiceEntry> entry : _watchServiceEntryMap.entrySet()) {
            try {
                entry.getValue()._watchService.close();
            } catch (Throwable e) {
                logger.error(null, e);
            }
        }
        
    }
    
    private class WatchServiceEntry {
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
                        File file = new File(_path, ((Path) event.context()).toString());
                        
                        if(file.isDirectory() 
                                && event.kind() == StandardWatchEventKinds.ENTRY_CREATE
                                ) {
                            _addDirToWatchRecursively(file);
                        }
                        
                        watchEventHandler.handleEvent(event, file);
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
