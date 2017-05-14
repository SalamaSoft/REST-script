package com.salama.service.script.config;

import java.io.Serializable;

public class ScriptProviderSetting implements Serializable {

    private static final long serialVersionUID = 6699847943015095397L;
    
    private String className = "";
    private String configLocation;
    
    public String getClassName() {
        return className;
    }
    public void setClassName(String className) {
        this.className = className;
    }
    public String getConfigLocation() {
        return configLocation;
    }
    public void setConfigLocation(String configLocation) {
        this.configLocation = configLocation;
    }
    
    
    
}
