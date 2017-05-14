package com.salama.service.script.config;

import java.io.Serializable;

public class ScriptServiceContextConfig implements Serializable {

    private static final long serialVersionUID = -9188805099606762839L;

    private String encoding = "utf-8";
    
    private ServletUploadSetting _servletUploadSetting = new ServletUploadSetting();

    private ScriptServiceDispatcherConfig serviceDispatcherConfig;

    
    public String getEncoding() {
        return encoding;
    }

    public void setEncoding(String encoding) {
        this.encoding = encoding;
    }

    public ServletUploadSetting getServletUploadSetting() {
        return _servletUploadSetting;
    }

    public void setServletUploadSetting(ServletUploadSetting servletUploadSetting) {
        _servletUploadSetting = servletUploadSetting;
    }

    public ScriptServiceDispatcherConfig getServiceDispatcherConfig() {
        return serviceDispatcherConfig;
    }

    public void setServiceDispatcherConfig(ScriptServiceDispatcherConfig serviceDispatcherConfig) {
        this.serviceDispatcherConfig = serviceDispatcherConfig;
    }

    
    
}
