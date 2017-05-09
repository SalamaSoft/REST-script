package com.salama.service.script.core;

import java.io.Reader;

public interface IScriptSourceWatcher {

    /**
     * (app, name) should be unique 
     * @param app ScriptEngines will be isolated between applications. 
     * @param name Be unique in one app. 
     * @param script source of script. It should return one object which implements methods of IScriptService.
     * 
     */
    void onSourceUpdated(String app, Reader script);
    
    /**
     * 
     * @param app
     * @param name
     */
    void onSourceDeleted(String app, String name);
}
