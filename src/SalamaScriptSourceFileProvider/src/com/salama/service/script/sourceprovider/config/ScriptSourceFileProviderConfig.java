package com.salama.service.script.sourceprovider.config;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class ScriptSourceFileProviderConfig implements Serializable {

    private static final long serialVersionUID = -3623303498891214113L;
    
    private String _globalSourceDir;
    private List<ScriptContextInitSetting> _globalScriptContextSettings = new ArrayList<>();
    
    private String _appSourceDir;
    private List<ScriptAppSetting> _appSettings = new ArrayList<>();

    public String getGlobalSourceDir() {
        return _globalSourceDir;
    }

    public void setGlobalSourceDir(String globalSourceDir) {
        _globalSourceDir = globalSourceDir;
    }

    public List<ScriptContextInitSetting> getGlobalScriptContextSettings() {
        return _globalScriptContextSettings;
    }

    public void setGlobalScriptContextSettings(List<ScriptContextInitSetting> globalScriptContextSettings) {
        _globalScriptContextSettings = globalScriptContextSettings;
    }

    public String getAppSourceDir() {
        return _appSourceDir;
    }

    public void setAppSourceDir(String appSourceDir) {
        _appSourceDir = appSourceDir;
    }

    public List<ScriptAppSetting> getAppSettings() {
        return _appSettings;
    }

    public void setAppSettings(List<ScriptAppSetting> appSettings) {
        _appSettings = appSettings;
    }

    
}
