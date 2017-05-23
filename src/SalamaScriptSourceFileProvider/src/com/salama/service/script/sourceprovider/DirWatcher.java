package com.salama.service.script.sourceprovider;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchService;
import java.util.concurrent.ConcurrentHashMap;

public class DirWatcher {
    //key:absolute path     value:WatchService
    private final ConcurrentHashMap<String, WatchService> _watcherMap = new ConcurrentHashMap<>();
    
    public DirWatcher() {
    }
    
    public void addDirToWatch(File dir) throws IOException {
        final WatchService pathWatcher = FileSystems.getDefault().newWatchService();
        
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
    }
}
