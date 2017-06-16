package com.salama.service.script.core;

/**
 * 
 * @param <T> Type of request
 */
public interface IServiceTargetFinder<T> extends IScriptContext, IServiceNameVerifier {

    ServiceTarget findOut(T request);

}
