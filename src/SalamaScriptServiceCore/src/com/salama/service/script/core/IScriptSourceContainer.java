package com.salama.service.script.core;

import javax.script.CompiledScript;

public interface IScriptSourceContainer extends IScriptContext {

    IScriptSourceWatcher getScriptSourceWatcher();
    
    CompiledScript findCompiledScript(ServiceTarget target);

}
