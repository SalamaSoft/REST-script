package com.salama.service.script.sourceprovider.config;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class ScriptSourceFileProviderConfig implements Serializable {

    private static final long serialVersionUID = -3623303498891214113L;

    /**
     * Directory for placing library files (.jar, .so, .dll, ... etc)
     */
    private String _extLibDir;    
    
    /**
     * File extension names(to filter files). Separated by comma if multiple ones.
     */
    private String _scriptFileExtFilter = ".js";
    
    private String _globalSourceDir;
    private List<ScriptInitSetting> _globalScriptInitSettings = new ArrayList<>();
    
    private String _appSourceDir;
    private List<ScriptAppSetting> _appSettings = new ArrayList<>();

    
    public String getExtLibDir() {
        return _extLibDir;
    }

    public void setExtLibDir(String extLibDir) {
        _extLibDir = extLibDir;
    }

    public String getScriptFileExtFilter() {
        return _scriptFileExtFilter;
    }

    public void setScriptFileExtFilter(String scriptFileExtFilter) {
        _scriptFileExtFilter = scriptFileExtFilter;
    }

    public String getGlobalSourceDir() {
        return _globalSourceDir;
    }

    public void setGlobalSourceDir(String globalSourceDir) {
        _globalSourceDir = globalSourceDir;
    }

    public List<ScriptInitSetting> getGlobalScriptInitSettings() {
        return _globalScriptInitSettings;
    }

    public void setGlobalScriptInitSettings(List<ScriptInitSetting> globalScriptInitSettings) {
        _globalScriptInitSettings = globalScriptInitSettings;
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
