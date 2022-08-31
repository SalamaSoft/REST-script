package com.salama.service.script.config;

import java.io.Serializable;

public class ServletUploadSetting implements Serializable {

    private static final long serialVersionUID = -5152091108216076495L;

    /**
     * Default: 5MB
     */
    private long _fileSizeMax = 5242880;
    
    /**
     * Default: 20MB
     */
    private long _sizeMax = 20971520;

    /**
     * Default: 512KB
     */
    private int _sizeThreshold = 524288;
    
    private String _tempDirPath = "";

    public long getFileSizeMax() {
        return _fileSizeMax;
    }

    public void setFileSizeMax(long fileSizeMax) {
        _fileSizeMax = fileSizeMax;
    }

    public long getSizeMax() {
        return _sizeMax;
    }

    public void setSizeMax(long sizeMax) {
        _sizeMax = sizeMax;
    }

    public int getSizeThreshold() {
        return _sizeThreshold;
    }

    public void setSizeThreshold(int sizeThreshold) {
        _sizeThreshold = sizeThreshold;
    }

    public String getTempDirPath() {
        return _tempDirPath;
    }

    public void setTempDirPath(String tempDirPath) {
        _tempDirPath = tempDirPath;
    }
    
    
}
