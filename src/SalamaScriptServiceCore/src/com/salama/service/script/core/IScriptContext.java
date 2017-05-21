package com.salama.service.script.core;

import java.io.IOException;
import java.io.Reader;

public interface IScriptContext extends IScriptService {
    
    void reload(Reader config, IConfigLocationResolver configLocationResolver) throws IOException;
    
    void destroy();

}
