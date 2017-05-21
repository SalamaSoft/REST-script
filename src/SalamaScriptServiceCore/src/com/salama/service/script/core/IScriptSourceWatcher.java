package com.salama.service.script.core;

import java.io.Reader;

import javax.script.ScriptException;

public interface IScriptSourceWatcher {

    /**
     * If app is not null, the obj will be put into ScriptEngine of the app.
     * Otherwise, the obj will be put into ScriptEngineManager as global variable.
     * @param Optional. app
     * @param Required. varName
     * @param obj Required. instance of Java object
     * @param config Optional. argument of config when the obj contains method 'reload(String config)' 
     */
    void onJavaObjUpdated(String app, String varName, Object obj, Reader config);
    
    /**
     * If app is not null, deleting operation is on ScriptEngine of the app.
     * Otherwise, deleting operation is on ScriptEngineManager.
     * @param Optional. app
     * @param Required. varName
     */
    void onJavaObjDeleted(String app, String varName);
    
    /**
     * This method is invoked when source of script updated.
     * If app is not null, the value returned by eval(script) will be put into ScriptEngine of the app.
     * Otherwise, the script object will be put into ScriptEngineManager as global variable with the key of IScriptService.serviceName().
     * @param Optional. app  
     * @param Required. script source of script. It should return one object which implements methods of IScriptService.
     * @param config Optional. argument of config when the obj contains method 'reload(String config)' 
     * 
     * @return name of the script
     */
    String onScriptSourceUpdated(String app, Reader script, Reader config) throws ScriptException;
    
    /**
     * This method is invoked when source of script deleted.
     * If app is not null, deleting operation is on ScriptEngine of the app.
     * Otherwise, deleting operation is on ScriptEngineManager.
     * @param app
     * @param name
     */
    void onScriptSourceDeleted(String app, String name);
}
