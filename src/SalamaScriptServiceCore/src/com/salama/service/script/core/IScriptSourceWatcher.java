package com.salama.service.script.core;

import java.io.Reader;
import java.util.List;

import javax.script.ScriptException;

public interface IScriptSourceWatcher {
    
    public static class InitLoadJavaEntry {
        private int entryNum;
        private String app;
        private String varName;
        private Object obj; 
        private Reader config;
        
        public InitLoadJavaEntry(
                int entryNum,
                String app, String varName, Object obj, Reader config
                ) {
            this.entryNum = entryNum;
            this.app = app;
            this.varName = varName;
            this.obj = obj;
            this.config = config;
        }
        
        public int getEntryNum() {
            return entryNum;
        }
        public String getApp() {
            return app;
        }
        public String getVarName() {
            return varName;
        }
        public Object getObj() {
            return obj;
        }
        public Reader getConfig() {
            return config;
        }

    } 
    
    public static class InitLoadScriptEntry {
        private int entryNum;
        private String app;
        private ITextFile script;
        private Reader config;
        
        public InitLoadScriptEntry(
                int entryNum,
                String app, ITextFile script, Reader config
                ) {
            this.entryNum = entryNum;
            this.app = app;
            this.script = script;
            this.config = config;
        }

        public int getEntryNum() {
            return entryNum;
        }
        
        public String getApp() {
            return app;
        }

        public ITextFile getScript() {
            return script;
        }

        public Reader getConfig() {
            return config;
        }
        
        
    }
    
    
    /**
     * This method is invoked when the provider initializing.
     * The initLoadEntries should be in order same as it in configuration.  
     * @param scriptEntries
     */
    void onInitLoadJavaObj(List<InitLoadJavaEntry> initLoadEntries);

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
     * This method is invoked when the provider initializing.
     * The initLoadEntries should be in order same as it in configuration.  
     * @param scriptEntries
     */
    void onInitLoadScriptSource(List<InitLoadScriptEntry> initLoadEntries);
    
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
    String onScriptSourceUpdated(String app, ITextFile script, Reader config) throws ScriptException;
    
    /**
     * This method is invoked when source of script deleted.
     * If app is not null, deleting operation is on ScriptEngine of the app.
     * Otherwise, deleting operation is on ScriptEngineManager.
     * @param app
     * @param name
     */
    void onScriptSourceDeleted(String app, String name);
}
