package com.salama.service.script.config;

import java.io.Serializable;

import com.salama.service.script.core.ScriptContextSetting;


public class ScriptServiceServletContextConfig implements Serializable {

    private static final long serialVersionUID = -9188805099606762839L;

    private String encoding = "utf-8";
    
    private ServletUploadSetting _servletUploadSetting = new ServletUploadSetting();

    private ScriptContextSetting _serviceDispatcherSetting;

    
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

    public ScriptContextSetting getServiceDispatcherSetting() {
        return _serviceDispatcherSetting;
    }

    public void setServiceDispatcherSetting(ScriptContextSetting serviceDispatcherSetting) {
        _serviceDispatcherSetting = serviceDispatcherSetting;
    }

    
}
