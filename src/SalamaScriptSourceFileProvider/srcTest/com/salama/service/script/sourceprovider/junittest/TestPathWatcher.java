package com.salama.service.script.sourceprovider.junittest;

import java.io.File;
import java.io.IOException;
import java.nio.file.ClosedWatchServiceException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.Timer;
import java.util.TimerTask;

import org.junit.Test;

public class TestPathWatcher {
    private Path _rootPath;

    @Test
    public void test1() {
        File file = new File("temp");
        
        try {
            FileSystem fs = FileSystems.getDefault();
            WatchService pathWatcher = fs.newWatchService();
            
            _rootPath = fs.getPath(file.getAbsolutePath());
            System.out.println("_rootPath:" + _rootPath.toAbsolutePath());
            _rootPath.register(
                    pathWatcher, 
                    StandardWatchEventKinds.ENTRY_CREATE,
                    StandardWatchEventKinds.ENTRY_DELETE,
                    StandardWatchEventKinds.ENTRY_MODIFY
                    );

            final Timer timer = new Timer();
            timer.schedule(new TimerTask() {
                
                @Override
                public void run() {
                    timer.cancel();
                    try {
                        System.out.println("watcher close.");
                        pathWatcher.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }, 30L * 1000);
            
            try {
                while(true) {
                    WatchKey watchKey = pathWatcher.take();

                    //handle events
                    for(WatchEvent<?> event : watchKey.pollEvents()) {
                        printWatchEvent(event);
                    }
                    
                    //reset key
                    boolean valid = watchKey.reset();
                    if(!valid) {
                        // object no longer registered
                        System.out.println("WatchKey.reset: " + valid);
                    }
                }
            } catch (ClosedWatchServiceException closedError) {
                System.out.println("watcher event loop end.");
            }
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }
    
    
    private void printWatchEvent(WatchEvent<?> event) throws IOException {
        Path path = (Path) event.context();

        System.out.println("watchEvent ->"
                + " count: " + event.count()
                + " kind: " + event.kind()
                + "\n_rootPath:" + _rootPath.toFile().getAbsolutePath()
                + "\npath     :" + path.toFile().getAbsolutePath()
                );
    }
}
