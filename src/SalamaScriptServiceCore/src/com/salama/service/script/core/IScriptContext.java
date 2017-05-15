package com.salama.service.script.core;

public interface IScriptContext {
    
    public final static String MethodName_reload = "reload";
    public final static String MethodName_destroy = "destroy";

    void reload(String config);
    
    void destroy();

}
