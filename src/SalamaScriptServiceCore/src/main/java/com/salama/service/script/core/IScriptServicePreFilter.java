package com.salama.service.script.core;

import java.util.Map;

public interface IScriptServicePreFilter extends IScriptService {

    public static class PreFilterResultKeys {
        public final static String override = "override";
        public final static String result = "result";
    }
    
    /**
     * 
     * @param target
     * @param params
     * @param request
     * @param response
     * @return {"override": Boolean,  "result": Object}
     */
    public Map<String, Object> doPreFilter(
            ServiceTarget target, 
            Map<String, Object> params, 
            Object request, Object response
            );
}
