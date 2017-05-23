package com.salama.service.script.sourceprovider.config;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class ScriptSourceFileProviderConfig implements Serializable {

    private static final long serialVersionUID = -3623303498891214113L;
    
    private String _globalSourceDir;
    
    private String _appsSourceDir;
    
    private List<ScriptAppSetting> _scriptAppSettings = new ArrayList<>();

    public String getGlobalSourceDir() {
        return _globalSourceDir;
    }

    public void setGlobalSourceDir(String globalSourceDir) {
        _globalSourceDir = globalSourceDir;
    }

    public String getAppsSourceDir() {
        return _appsSourceDir;
    }

    public void setAppsSourceDir(String appsSourceDir) {
        _appsSourceDir = appsSourceDir;
    }

    public List<ScriptAppSetting> getScriptAppSettings() {
        return _scriptAppSettings;
    }

    public void setScriptAppSettings(List<ScriptAppSetting> scriptAppSettings) {
        _scriptAppSettings = scriptAppSettings;
    }

    
    
}
