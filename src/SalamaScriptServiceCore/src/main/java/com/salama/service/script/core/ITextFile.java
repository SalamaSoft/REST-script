package com.salama.service.script.core;

import java.io.IOException;
import java.io.Reader;

/**
 * Abstract script file to support variant SourceProvider
 *
 */
public interface ITextFile {

    /**
     * Abstract path. (Could be file path or other type)
     * @return
     */
    String getPath();
    
    Reader getReader() throws IOException;
        
}
