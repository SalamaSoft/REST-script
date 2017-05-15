package com.salama.service.script.core;

public interface IScriptService {
    public final static String MethodName_serviceName = "serviceName";
    public final static String MethodName_version = "version";

    /**
     * 
     * @return For matching the request URI.
     */
    String serviceName();
    
    /**
     * 
     * @return Not important, just for logging
     */
    String version();
}
