package testscriptsourcecontainer;

import java.io.File;
import java.io.IOException;

public class TestScriptContext {
    private SimpleScriptDispatcher _dispatcher;
    
    private static TestScriptContext _singleton;
    public static TestScriptContext singleton() {
        return _singleton;
    }
    
    public SimpleScriptDispatcher getDispatcher() {
        return _dispatcher;
    }
    
    public void init() throws IOException {
        _singleton = this;
        
        //current workdir
        File scriptRootDir = new File("script_root");
        final SimpleScriptDispatcher dispatcher = new SimpleScriptDispatcher(
                scriptRootDir, SimpleScriptDispatcher.defaultServiceNameVerifier()  
                );
        
        _dispatcher = dispatcher;
    }
    
    public void destroy() throws IOException {
        _dispatcher.close();
    }
}
