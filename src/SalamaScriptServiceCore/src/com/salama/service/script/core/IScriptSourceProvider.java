package com.salama.service.script.core;

public interface IScriptSourceProvider extends IScriptContext {
    
    void addWatcher(IScriptSourceWatcher watcher);
        
}
