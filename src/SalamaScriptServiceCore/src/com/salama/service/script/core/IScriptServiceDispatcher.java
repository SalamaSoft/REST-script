package com.salama.service.script.core;

import javax.script.ScriptException;

/**
 * 
 * @param <ReqT> Type of request
 * @param <RespT> Type of response
 */
public interface IScriptServiceDispatcher<ReqT, RespT> extends IScriptContext {

    IServiceTargetFinder<ReqT> getServiceTargetFinder();
    
    Object dispatch(ReqT request, RespT response) throws ScriptException, NoSuchMethodException;
    
}
