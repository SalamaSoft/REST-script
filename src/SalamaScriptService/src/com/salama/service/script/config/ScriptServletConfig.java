package com.salama.service.script.config;

public class ScriptServletConfig {

    private String scriptEngineName = "nashorn";
    private ScriptContextSetting scriptSourceProviderSetting = new ScriptContextSetting();

    private String serviceTargetFinder = null;
    
    public String getScriptEngineName() {
        return scriptEngineName;
    }

    public void setScriptEngineName(String scriptEngineName) {
        this.scriptEngineName = scriptEngineName;
    }

    public ScriptContextSetting getScriptSourceProviderSetting() {
        return scriptSourceProviderSetting;
    }

    public void setScriptSourceProviderSetting(ScriptContextSetting scriptSourceProviderSetting) {
        this.scriptSourceProviderSetting = scriptSourceProviderSetting;
    }

    public String getServiceTargetFinder() {
        return serviceTargetFinder;
    }

    public void setServiceTargetFinder(String serviceTargetFinder) {
        this.serviceTargetFinder = serviceTargetFinder;
    }

    
    
}
