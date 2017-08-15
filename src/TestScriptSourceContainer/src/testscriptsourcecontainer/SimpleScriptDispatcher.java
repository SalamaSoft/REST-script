package testscriptsourcecontainer;

import java.io.Closeable;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.regex.Pattern;

import javax.script.CompiledScript;
import javax.script.Invocable;
import javax.script.ScriptException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.salama.service.script.core.IConfigLocationResolver;
import com.salama.service.script.core.IScriptSourceContainer;
import com.salama.service.script.core.IScriptSourceProvider;
import com.salama.service.script.core.IServiceNameVerifier;
import com.salama.service.script.core.ServiceTarget;
import com.salama.service.script.sourcecontainer.ScriptSourceContainer;
import com.salama.service.script.sourceprovider.ScriptSourceFileProvider;

public class SimpleScriptDispatcher implements Closeable {
    private final static Log logger = LogFactory.getLog(SimpleScriptDispatcher.class);

    public final static String DEFAULT_SCRIPT_ENGINE = "nashorn";
    
    private final static String VarName_Dispatcher = "$dispatcher";
    private final static String CONFIG_FILE_SCRIPT_SOURCE_PROVIDER = "config/ScriptSourceFileProviderConfig.xml";
    
    private final File _scriptRoot;

    
    private IConfigLocationResolver _configLocationResolver;
    private IScriptSourceProvider _scriptSourceProvider;
    private IScriptSourceContainer _scriptSourceContainer;
    
    public static IServiceNameVerifier defaultServiceNameVerifier() {
        return new IServiceNameVerifier() {
            private final static String REGEX_APP = "[a-zA-Z0-9\\-_\\.]+";
            private final static String REGEX_SERVICE_NAME = "[a-zA-Z0-9\\-_\\.]+";
            
            private final Pattern _patternApp = Pattern.compile(REGEX_APP);
            private final Pattern _patternServiceName = Pattern.compile(REGEX_SERVICE_NAME);
            
            
            @Override
            public boolean verifyFormatOfApp(String app) {
                return _patternApp.matcher(app).matches();
            }

            @Override
            public boolean verifyFormatOfServiceName(String serviceName) {
                return _patternServiceName.matcher(serviceName).matches();
            }
            
        };
    }
    
    public SimpleScriptDispatcher(File scriptRootDir, IServiceNameVerifier serviceNameVerifier) throws IOException {
        this(null, scriptRootDir, serviceNameVerifier);
    }
    
    public SimpleScriptDispatcher(String scriptEngineName, File scriptRootDir, IServiceNameVerifier serviceNameVerifier) throws IOException {
        _scriptRoot = new File(scriptRootDir.getAbsolutePath());
        
        _configLocationResolver = new IConfigLocationResolver() {
            
            @Override
            public Reader resolveConfigLocation(String configLocation) throws IOException {
                if(configLocation == null || configLocation.trim().length() == 0) {
                    return null;
                }
                
                return new FileReader(new File(_scriptRoot, configLocation));
            }
        };
        
        _scriptSourceProvider = new ScriptSourceFileProvider();
        //init source container
        _scriptSourceContainer = new ScriptSourceContainer();
        _scriptSourceContainer.init(
                (scriptEngineName != null && scriptEngineName.length() > 0) ? scriptEngineName : DEFAULT_SCRIPT_ENGINE, 
                serviceNameVerifier, 
                _configLocationResolver
                );
        
        //my global variables
        initGlobalVar();
        
        //add watcher
        _scriptSourceProvider.addWatcher(_scriptSourceContainer);
        
        //reload
        Reader configReader = new FileReader(new File(_scriptRoot, CONFIG_FILE_SCRIPT_SOURCE_PROVIDER));
        try {
            _scriptSourceProvider.reload(
                    configReader, 
                    _configLocationResolver
                    );
        } finally {
            configReader.close();
        }
    }
    
    @Override
    public void close() throws IOException {
        //destroy 
        try {
            _scriptSourceContainer.close();
        } catch(Throwable e) {
            logger.error(null, e);
        }
        
        try {
            _scriptSourceProvider.destroy();
        } catch(Throwable e) {
            logger.error(null, e);
        }
        
        logger.info("scrip dispatcher destroy finished ------");
    }
    
    public Object call(ServiceTarget target, Object... params) throws NoSuchMethodException, ScriptException {
        final CompiledScript compiledScript = _scriptSourceContainer.findCompiledScript(target);
        
        //invoke service
        final Object serviceObj = compiledScript.eval();
        final Object result = ((Invocable) compiledScript.getEngine()).invokeMethod(
                serviceObj, target.method, 
                params
                );
        return result;
    }
    
    private void initGlobalVar() {
        _scriptSourceContainer.onJavaObjUpdated(
                null, VarName_Dispatcher, 
                new ScriptEngineBridge(), 
                null
                );
    }
    
    public class ScriptEngineBridge {
        
        public ServiceTarget buildTarget(String app, String service, String method) {
            ServiceTarget target = new ServiceTarget();
            target.app = app;
            target.service = service;
            target.method = method;
            
            return target;
        }
        
        public boolean hasTarget(ServiceTarget target) {
            final CompiledScript compiledScript = _scriptSourceContainer.findCompiledScript(target);
            
            return (compiledScript != null);
        }
        
        public Object call(
                ServiceTarget target,
                Object params, 
                Object request, Object response 
                ) throws NoSuchMethodException, ScriptException {
            if(target.app == null || target.app.length() == 0) {
                throw new IllegalArgumentException(
                        "app of target should not be empty."
                        + " app:" + target.app 
                        + " service:" + target.service 
                        + " method:" + target.method
                        );
            }
            
            CompiledScript compiledScript = _scriptSourceContainer.findCompiledScript(target);
            Object serviceObj = compiledScript.eval();
            
            return ((Invocable) compiledScript.getEngine()).invokeMethod(
                    serviceObj, target.method, 
                    params,
                    request, response
                    );
        }
        
    }
    
}

