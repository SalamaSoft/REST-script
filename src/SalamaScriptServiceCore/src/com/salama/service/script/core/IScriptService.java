package com.salama.service.script.core;

public interface IScriptService {

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
