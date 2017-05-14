package com.salama.service.script.sourceprovider;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import com.salama.service.script.core.IScriptSourceProvider;
import com.salama.service.script.core.IScriptSourceWatcher;

public class BaseScriptSourceProvider implements IScriptSourceProvider {
    private final List<IScriptSourceWatcher> _scriptWatchers = new ArrayList<IScriptSourceWatcher>();

    @Override
    public void addWatcher(IScriptSourceWatcher watcher) {
        _scriptWatchers.add(watcher);
    }

    @Override
    public void reload(File configFile) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void destroy() {
        // TODO Auto-generated method stub
        
    }


}
