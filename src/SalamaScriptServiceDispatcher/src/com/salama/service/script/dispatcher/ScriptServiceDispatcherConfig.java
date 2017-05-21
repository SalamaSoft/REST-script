package com.salama.service.script.dispatcher;

import java.io.Serializable;

import com.salama.service.script.core.ScriptContextSetting;

public class ScriptServiceDispatcherConfig implements Serializable {
    private static final long serialVersionUID = 4895612496118184116L;
    
    private String scriptEngineName = "nashorn";
    private ScriptContextSetting scriptSourceProvider = null;
    private ScriptContextSetting serviceTargetFinder = null;
    
    public String getScriptEngineName() {
        return scriptEngineName;
    }
    public void setScriptEngineName(String scriptEngineName) {
        this.scriptEngineName = scriptEngineName;
    }
    public ScriptContextSetting getScriptSourceProvider() {
        return scriptSourceProvider;
    }
    public void setScriptSourceProvider(ScriptContextSetting scriptSourceProvider) {
        this.scriptSourceProvider = scriptSourceProvider;
    }
    public ScriptContextSetting getServiceTargetFinder() {
        return serviceTargetFinder;
    }
    public void setServiceTargetFinder(ScriptContextSetting serviceTargetFinder) {
        this.serviceTargetFinder = serviceTargetFinder;
    }
    

}
