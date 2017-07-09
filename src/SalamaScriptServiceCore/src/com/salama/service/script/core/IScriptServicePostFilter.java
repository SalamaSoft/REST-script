package com.salama.service.script.core;

public interface IScriptServicePostFilter extends IScriptService {

    public static class PreFilterResultKeys {
        public final static String override = "override";
        public final static String result = "result";
    }

    /**
     * 
     * @param target
     * @param result
     * @param request
     * @param response
     * @return transformed result
     */
    public Object doPostFilter(
            ServiceTarget target, 
            Object result, 
            Object request, Object response
            );
}
