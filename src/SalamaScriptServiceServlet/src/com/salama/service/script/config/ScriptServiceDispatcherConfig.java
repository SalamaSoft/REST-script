package com.salama.service.script.config;

import java.io.Serializable;

public class ScriptServiceDispatcherConfig implements Serializable {
    private static final long serialVersionUID = 4895612496118184116L;
    
    private String scriptEngineName = "nashorn";
    private ScriptProviderSetting scriptSourceProviderSetting = new ScriptProviderSetting();
    private String serviceTargetFinder = null;
    
    public String getScriptEngineName() {
        return scriptEngineName;
    }

    public void setScriptEngineName(String scriptEngineName) {
        this.scriptEngineName = scriptEngineName;
    }

    public ScriptProviderSetting getScriptSourceProviderSetting() {
        return scriptSourceProviderSetting;
    }

    public void setScriptSourceProviderSetting(ScriptProviderSetting scriptSourceProviderSetting) {
        this.scriptSourceProviderSetting = scriptSourceProviderSetting;
    }

    public String getServiceTargetFinder() {
        return serviceTargetFinder;
    }

    public void setServiceTargetFinder(String serviceTargetFinder) {
        this.serviceTargetFinder = serviceTargetFinder;
    }

}
