package com.salama.service.script.core;

import java.io.File;

public interface IScriptSourceProvider {

    public void reload(File configFile);
    
    public void destroy();
    
    void addWatcher(IScriptSourceWatcher watcher);
        
}
