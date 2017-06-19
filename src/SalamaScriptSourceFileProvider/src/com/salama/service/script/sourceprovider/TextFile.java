package com.salama.service.script.sourceprovider;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.Charset;

import com.salama.service.script.core.ITextFile;

public class TextFile implements ITextFile {

    private final Charset _charset;
    private final File _file;
    
    public TextFile(File file, Charset charset) {
        _file = file;
        _charset = charset;
    }

    @Override
    public String getPath() {
        return _file.getAbsolutePath();
    }

    @Override
    public Reader getReader() throws IOException {
        return new InputStreamReader(new FileInputStream(_file), _charset);
    }
    
    
}
