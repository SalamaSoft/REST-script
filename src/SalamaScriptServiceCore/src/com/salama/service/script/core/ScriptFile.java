package com.salama.service.script.core;

import java.io.Reader;

/**
 * Abstract script file to support variant SourceProvider
 *
 */
public class ScriptFile {

    private final String path;
    private final Reader script;
    
    
    /**
     * 
     * @param path Abstract path. (Could be file path or other URL format)
     * @param script
     */
    public ScriptFile(String path, Reader script) {
        this.path = path;
        this.script = script;
    }


    public String getPath() {
        return path;
    }


    public Reader getScript() {
        return script;
    }
    
}
