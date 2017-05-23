package com.salama.service.script.sourceprovider.config;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class ScriptAppSetting implements Serializable {

    private static final long serialVersionUID = 4609165801777611427L;

    private String appId;
    
    private List<ScriptContextInitSetting> _scriptContextInitSettings = new ArrayList<>();

    public String getAppId() {
        return appId;
    }

    public void setAppId(String appId) {
        this.appId = appId;
    }

    public List<ScriptContextInitSetting> getScriptContextInitSettings() {
        return _scriptContextInitSettings;
    }

    public void setScriptContextInitSettings(List<ScriptContextInitSetting> scriptContextInitSettings) {
        _scriptContextInitSettings = scriptContextInitSettings;
    }
       
    
}
