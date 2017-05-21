package com.salama.service.script.core;

import javax.script.CompiledScript;
import javax.script.ScriptException;

import com.salama.service.core.net.RequestWrapper;
import com.salama.service.core.net.ResponseWrapper;

public interface IScriptServiceDispatcher extends IScriptContext {

    IScriptSourceWatcher getScriptSourceWatcher();
    
    IServiceTargetFinder getServiceTargetFinder();
    
    Object dispatch(RequestWrapper request, ResponseWrapper response) throws ScriptException, NoSuchMethodException;
    
    CompiledScript findCompiledScript(ServiceTarget target);
    
}
