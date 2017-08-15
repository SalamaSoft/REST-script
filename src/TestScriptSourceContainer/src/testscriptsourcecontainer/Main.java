package testscriptsourcecontainer;

import java.io.File;
import java.io.IOException;

import javax.script.ScriptException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.salama.service.script.core.IServiceNameVerifier;
import com.salama.service.script.core.ServiceTarget;

public class Main {
    private final static Log logger = LogFactory.getLog(Main.class);

    
    public static void main(String[] args) {
        TestScriptContext context = null;
        try {
            context = new TestScriptContext();
            context.init();
            
            testAll();
            
        } catch (Throwable e) {
            logger.error(null, e);
        } finally {
            try {
                context.destroy();
            } catch (Throwable e) {
                logger.error(null, e);
            }
        }
    }
    
    private static ServiceTarget buildTarget(String app, String service, String method) {
        ServiceTarget target = new ServiceTarget();
        target.app = app;
        target.service = service;
        target.method = method;
        
        return target;
    }
    
    private static void printObject(Object obj) {
        String msg;
        if(obj != null) {
            msg = "class:" + obj.getClass().getName() + " value:" + obj; 
        } else {
            msg = "value:" + obj;
        }
        
        logger.debug(msg);
    }
    
    private static void testAll() throws NoSuchMethodException, ScriptException {
        test1();
    }
    
    private static void test1() throws NoSuchMethodException, ScriptException {
        Object result = TestScriptContext.singleton().getDispatcher().call(
                buildTarget("app1", "Test1", "test"), 
                "arg0", 199, "Test----"
                );
        printObject(result);
    }
    
}
