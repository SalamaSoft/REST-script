package com.salama.service.script.core;

import java.io.Reader;

public interface IScriptSourceWatcher {

    /**
     * This method is invoked when global variable updated.
     * Global variable will be added into ScriptEngineManager and then it can be invoked in all script.
     * @param varName name of variable
     * @param obj instance of obj
     */
    void onGlobalVarUpdated(String varName, Object obj);
    
    /**
     * This method is invoked when global variable deleted.
     * @param varName
     */
    void onGlobalVarDeleted(String varName);
    
    /**
     * This method is invoked when source of script updated.
     * (app, name) should be unique 
     * @param app ScriptEngines will be isolated between applications. 
     * @param name Be unique in one app. 
     * @param script source of script. It should return one object which implements methods of IScriptService.
     * 
     */
    void onScriptSourceUpdated(String app, Reader script);
    
    /**
     * This method is invoked when source of script deleted.
     * @param app
     * @param name
     */
    void onScriptSourceDeleted(String app, String name);
}
