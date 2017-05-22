package com.salama.service.script.core;

import java.io.IOException;
import java.io.Reader;

public interface IConfigLocationResolver {

    Reader resolveConfigLocation(String configLocation) throws IOException;
}
