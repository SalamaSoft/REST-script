package com.salama.service.script;

import org.apache.log4j.Logger;

import com.salama.service.script.core.IScriptServletUriParser;
import com.salama.service.script.core.ScriptServiceNamespace;

class DefaultScriptServletUriParser implements IScriptServletUriParser {
    private final static Logger logger = Logger.getLogger(DefaultScriptServletUriParser.class);
    
    public DefaultScriptServletUriParser() {
        logger.info(
                "DefaultScriptServletUriParser URI pattern($serviceName is only [a-zA-Z0-9\\-\\_\\.].+') ->"
                + " /$contextPath/$servlet_uri/$app/$serviceName"
                );
    }

    @Override
    public ScriptServiceNamespace parseURI(String uri) {
        int lastSlash = uri.lastIndexOf('/');
        int last2ndSlash = uri.lastIndexOf('/', lastSlash - 1);
        
        ScriptServiceNamespace np = new ScriptServiceNamespace();
        np.app = uri.substring(last2ndSlash + 1, lastSlash);
        np.serviceName = uri.substring(lastSlash + 1);
        
        return np;
    }

}
