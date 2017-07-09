package com.salama.service.script;

import java.io.IOException;
import java.io.Reader;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;

import org.apache.log4j.Logger;

import com.salama.service.core.net.RequestWrapper;
import com.salama.service.script.core.IConfigLocationResolver;
import com.salama.service.script.core.IServiceTargetFinder;
import com.salama.service.script.core.ServiceTarget;

public class UriServiceTargetFinder implements IServiceTargetFinder<RequestWrapper> {
    private final static Logger logger = Logger.getLogger(UriServiceTargetFinder.class);
    
    private final static String REGEX_APP = "[a-zA-Z0-9\\-_\\.]+";
    private final static String REGEX_SERVICE_NAME = "[a-zA-Z0-9\\-_\\.]+";
    
    private final Pattern _patternApp = Pattern.compile(REGEX_APP);
    private final Pattern _patternServiceName = Pattern.compile(REGEX_SERVICE_NAME);
    
    public UriServiceTargetFinder() {
        logger.info(
                "ServiceTargetFinder ->"
                + " uri format: " + "/*/.../*/$app/$serviceName.$serviceMethod"
                + " app pattern: " + REGEX_APP
                + " serviceName pattern: " + REGEX_SERVICE_NAME
                );
    }

    @Override
    public ServiceTarget findOut(RequestWrapper request) {
        final String uri = ((HttpServletRequest) request.getRequest()).getRequestURI();
        
        int lastSlash = uri.lastIndexOf('/');
        int last2ndSlash = uri.lastIndexOf('/', lastSlash - 1);
        
        ServiceTarget target = new ServiceTarget();
        target.app = uri.substring(last2ndSlash + 1, lastSlash);
        
        String serviceMethod = uri.substring(lastSlash + 1);
        int lastDot = serviceMethod.lastIndexOf('.');
        String service = serviceMethod.substring(0, lastDot);
        String method = serviceMethod.substring(lastDot + 1);
        
        target.service = service;
        target.method = method;
        
        return target;
    }

    @Override
    public boolean verifyFormatOfApp(String app) {
        return _patternApp.matcher(app).matches();
    }

    @Override
    public boolean verifyFormatOfServiceName(String serviceName) {
        return _patternServiceName.matcher(serviceName).matches();
    }

    @Override
    public void reload(Reader config, IConfigLocationResolver configLocationResolver) throws IOException {
    }

    @Override
    public void destroy() {
    }

    @Override
    public String serviceName() {
        return null;
    }


}
