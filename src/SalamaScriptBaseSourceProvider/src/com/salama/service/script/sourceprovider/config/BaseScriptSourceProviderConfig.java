package com.salama.service.script.sourceprovider.config;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class BaseScriptSourceProviderConfig implements Serializable {

    private static final long serialVersionUID = -3623303498891214113L;
    
    private String _sourceStorageDir;
    
    private List<ScriptServiceAppConfig> serviceAppConfigs = new ArrayList<>();

    public String getSourceStorageDir() {
        return _sourceStorageDir;
    }

    public void setSourceStorageDir(String sourceStorageDir) {
        _sourceStorageDir = sourceStorageDir;
    }

    public List<ScriptServiceAppConfig> getServiceAppConfigs() {
        return serviceAppConfigs;
    }

    public void setServiceAppConfigs(List<ScriptServiceAppConfig> serviceAppConfigs) {
        this.serviceAppConfigs = serviceAppConfigs;
    }
    
    
}
