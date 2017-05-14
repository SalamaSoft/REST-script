package com.salama.service.script.sourceprovider.config;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class AppAuthManagerSetting implements Serializable {

    private static final long serialVersionUID = -342498618525328822L;

    private String className = "";
    
    private List<String> paramTypes = new ArrayList<String>();
    
    private List<String> paramValues = new ArrayList<String>();

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public List<String> getParamTypes() {
        return paramTypes;
    }

    public void setParamTypes(List<String> paramTypes) {
        this.paramTypes = paramTypes;
    }

    public List<String> getParamValues() {
        return paramValues;
    }

    public void setParamValues(List<String> paramValues) {
        this.paramValues = paramValues;
    }
    
}
