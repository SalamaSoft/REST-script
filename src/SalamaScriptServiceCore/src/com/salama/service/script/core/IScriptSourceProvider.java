package com.salama.service.script.core;

import java.io.File;

public interface IScriptSourceProvider {

    void reload(File configFile);

    void destroy();
    
    void addWatcher(IScriptSourceWatcher watcher);
        
}
