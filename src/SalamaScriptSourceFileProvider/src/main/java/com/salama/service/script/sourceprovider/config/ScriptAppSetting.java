package com.salama.service.script.sourceprovider.config;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class ScriptAppSetting implements Serializable {

    private static final long serialVersionUID = 4609165801777611427L;

    private String app;
    
    private List<ScriptInitSetting> _scriptInitSettings = new ArrayList<>();


    public String getApp() {
        return app;
    }

    public void setApp(String app) {
        this.app = app;
    }

    public List<ScriptInitSetting> getScriptInitSettings() {
        return _scriptInitSettings;
    }

    public void setScriptInitSettings(List<ScriptInitSetting> scriptInitSettings) {
        _scriptInitSettings = scriptInitSettings;
    }

       
    
}
