package com.salama.service.script.core;

import java.io.Reader;

public interface IScriptContext {

    void reload(Reader configReader);
    
    void destroy();

}
