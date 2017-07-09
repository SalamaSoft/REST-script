package com.salama.service.script.core;

import java.io.Closeable;

import javax.script.CompiledScript;

/**
 * Manage those compiled script object
 * @author XingGu Liu
 *
 */
public interface IScriptSourceContainer extends IScriptSourceWatcher, Closeable {

    void init(String scriptEngineName, IServiceNameVerifier serviceNameVerifier, IConfigLocationResolver configLocationResolver);
    
    CompiledScript findCompiledScript(ServiceTarget target);
    
    IScriptServicePreFilter getPreFilter(ServiceTarget target);
    
    IScriptServicePostFilter getPostFilter(ServiceTarget target);

}
