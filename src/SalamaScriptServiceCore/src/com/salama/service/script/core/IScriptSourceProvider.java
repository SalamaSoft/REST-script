package com.salama.service.script.core;

import com.salama.service.core.context.CommonContext;

public interface IScriptSourceProvider extends CommonContext {

    
    void addWatcher(IScriptSourceWatcher watcher);
        
}
