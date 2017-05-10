package com.salama.service.script.config;

public class ScriptServletConfig {

    private String scriptEngineName = "nashorn";
    private ScriptContextSetting scriptSourceProviderSetting = new ScriptContextSetting();

    private String scriptServletUriParserClassName = null;
    
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

    public String getScriptServletUriParserClassName() {
        return scriptServletUriParserClassName;
    }

    public void setScriptServletUriParserClassName(String scriptServletUriParserClassName) {
        this.scriptServletUriParserClassName = scriptServletUriParserClassName;
    }
    
    
}
