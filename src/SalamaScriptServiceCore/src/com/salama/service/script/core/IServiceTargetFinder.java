package com.salama.service.script.core;

import com.salama.service.core.net.RequestWrapper;

public interface IServiceTargetFinder extends IScriptContext {

    ServiceTarget findOut(RequestWrapper request);

    boolean verifyFormatOfApp(String app);
    
    boolean verifyFormatOfServiceName(String serviceName);
}
