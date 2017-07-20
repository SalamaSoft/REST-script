package com.salama.service.script.sourceprovider;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.io.Reader;
import java.lang.reflect.InvocationTargetException;
import java.net.URLClassLoader;
import java.nio.charset.Charset;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.salama.service.script.core.IConfigLocationResolver;
import com.salama.service.script.core.IScriptSourceProvider;
import com.salama.service.script.core.IScriptSourceWatcher;
import com.salama.service.script.core.IScriptSourceWatcher.InitLoadScriptEntry;
import com.salama.service.script.sourceprovider.DirWatcher.IWatchEventHandler;
import com.salama.service.script.sourceprovider.config.ScriptAppSetting;
import com.salama.service.script.sourceprovider.config.ScriptInitSetting;
import com.salama.service.script.sourceprovider.config.ScriptSourceFileProviderConfig;

import MetoXML.XmlDeserializer;
import MetoXML.Base.XmlParseException;
import MetoXML.Util.ClassFinder;

public class ScriptSourceFileProvider implements IScriptSourceProvider {
    private final static Log logger = LogFactory.getLog(ScriptSourceFileProvider.class);
    
    
    public final static String DEFAULT_CHARSET_NAME = "utf-8";
    private final static Charset DEFAULT_CHARSET = Charset.forName(DEFAULT_CHARSET_NAME);
    
    private final static String APP_NAME_GLOBAL = "$";    
    
    private final List<IScriptSourceWatcher> _scriptWatchers = new ArrayList<IScriptSourceWatcher>();
    
    private IConfigLocationResolver _configLocationResolver;
    private ScriptSourceFileProviderConfig _config;
    //key:app       value: source directory of app
    private Map<String, String> _appSourceDirMap;
    private DirWatcher _dirWatcher;
    private Thread _watchThread;
    private File _globalSourceDir = null;
    private File _appSourceDir = null;
    
    private String[] _scriptFileExtFilterNames;
    private AppScriptLocationMap _appScriptLocationMap = new AppScriptLocationMap();
    private ClassLoader _classLoader = null;
    
    @Override
    public void addWatcher(IScriptSourceWatcher watcher) {
        _scriptWatchers.add(watcher);
    }

    @Override
    public ClassLoader getClassLoader() {
        return _classLoader;
    }
    
    @Override
    public void reload(Reader config, IConfigLocationResolver configLocationResolver) throws IOException {
        try {
            _config = (ScriptSourceFileProviderConfig) XmlDeserializer.stringToObject(
                    readText(config), 
                    ScriptSourceFileProviderConfig.class, 
                    new ClassFinder() {
                        
                        @Override
                        public Class<?> findClass(String className) throws ClassNotFoundException {
                            if(className.equals(ScriptInitSetting.class.getSimpleName())) {
                                return ScriptInitSetting.class;
                            } else if (className.equals(ScriptAppSetting.class.getSimpleName())) { 
                                return ScriptAppSetting.class;
                            } else {
                                return null;
                            }
                        }
                    }
                    );
            
            _globalSourceDir = new File(_config.getGlobalSourceDir());
            if(!_globalSourceDir.exists() || !_globalSourceDir.isDirectory()) {
                throw new IOException("GlobalSourceDir must be an existing directory ->" + _config.getGlobalSourceDir());
            }
            
            _appSourceDir = new File(_config.getAppSourceDir());
            if(!_appSourceDir.exists() || !_appSourceDir.isDirectory()) {
                throw new IOException("AppSourceDir must be an existing directory ->" + _config.getAppSourceDir());
            }
            
            if(_globalSourceDir.getAbsolutePath().startsWith(_appSourceDir.getAbsolutePath())
                    || _appSourceDir.getAbsolutePath().startsWith(_globalSourceDir.getAbsolutePath())
                    ) {
                throw new IOException("GlobalSourceDir and AppSourceDir must be in disjoint directory. ->"
                        + " appSourceDir:" + _config.getAppSourceDir() 
                        + " globalSourceDir:" + _config.getGlobalSourceDir()
                        );
            }
            
            _configLocationResolver = configLocationResolver;
            
            _scriptFileExtFilterNames = Arrays.asList(
                    _config.getScriptFileExtFilter()
                    .split("[ \\t]*,[ \\t]*")
                    ).stream()
                    .map(s -> s.trim())
                    .filter(s -> s.length() > 0)
                    .toArray(String[]::new)
                    ;
            
            logger.info("ScriptSourceFileProvider reload() start ->"
                    + "\nGlobalSourceDir:" + _config.getGlobalSourceDir()
                    + "\nAppSourceDir:" + _config.getAppSourceDir()
                    );
            
            loadExtLib();
            
            initDirWatcher();
            
            initLoadScriptFiles();
            
            //init watchThread
            final IWatchEventHandler watchEventHandler = new IWatchEventHandler() {
                
                @Override
                public void handleEvent(WatchEvent<?> event, File file) {
                    handleDirWatchEvent(event.kind(), file);
                }
            };
            _watchThread = new Thread(new Runnable() {
                
                @Override
                public void run() {
                    try {
                        while(true) {
                            _dirWatcher.pollEvent(watchEventHandler);
                            
                            Thread.sleep(200);
                        }
                    } catch (Throwable e) {
                        //Normally the exception is ClosedWatchServiceException or InterruptedException 
                        logger.info("WatchThread loop end", e);
                    }
                }
            });
            _watchThread.start();
            
            logger.info("ScriptSourceFileProvider reload() done ->"
                    + " GlobalSourceDir:" + _config.getGlobalSourceDir()
                    );
        } catch (InvocationTargetException | IllegalAccessException | InstantiationException | NoSuchMethodException
                | XmlParseException 
                e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void destroy() {
        logger.info("ScriptSourceFileProvider destroy() start");
        
        try {
            _scriptWatchers.clear();
        } catch (Throwable e) {
            logger.error(null, e);
        }
        
        try {
            _watchThread.interrupt();
        } catch (Throwable e) {
            logger.error(null, e);
        }
        
        try {
            _dirWatcher.close();
        } catch (Throwable e) {
            logger.error(null, e);
        }
        
        try {
            _appScriptLocationMap.clear();
        } catch (Throwable e) {
            logger.error(null, e);
        }
        
        logger.info("ScriptSourceFileProvider destroy() done");
    }

    @Override
    public String serviceName() {
        return null;
    }
    
    private void loadExtLib() {
        try {
            debugClassLoader();
            
            if(_config.getExtLibDir() == null 
                    || _config.getExtLibDir().trim().length() == 0) {
                return ;
            }
            File extLibDir = new File(_config.getExtLibDir());
            
            /*
            ClassLoader myClassLoader = ClassLoaderUtil.loadJarsInDir(
                    Thread.currentThread().getContextClassLoader(), 
                    extLibDir
                    );
            Thread.currentThread().setContextClassLoader(myClassLoader);
            _classLoader = myClassLoader;
            */
            
            ClassLoaderUtil.loadJarsIntoUrlClassLoader(
                    (URLClassLoader) Thread.currentThread().getContextClassLoader(), 
                    extLibDir
                    );
            
            ClassLoaderUtil.addLibraryPath(extLibDir);
        } catch (Throwable e) {
            logger.error(null, e);
        }
    }
    
    private void debugClassLoader() {
        logger.info("this.classLoader:" + this.getClass().getClassLoader());
        logger.info("curThread.classloader:" + Thread.currentThread().getContextClassLoader());
        ClassLoader cl = Thread.currentThread().getContextClassLoader().getParent();
        while(cl != null) {
            logger.info("parent_classloader:" + cl);
            
            cl = cl.getParent();
        }
    }
    
    private void initDirWatcher() throws IOException {
        _appSourceDirMap = new HashMap<>();
        
        _dirWatcher = new DirWatcher(true);
        _dirWatcher.addDirToWatch(getGlobalSourceDir());
        
        if(_config.getAppSettings() != null) {
            for(ScriptAppSetting appSetting : _config.getAppSettings()) {
                File appSourceDir = getAppSourceDir(appSetting.getApp());
                
                _dirWatcher.addDirToWatch(appSourceDir);
                _appSourceDirMap.put(appSetting.getApp(), appSourceDir.getAbsolutePath());
            }
        }
        
        //scriptName -> scriptInitSetting
        for(ScriptInitSetting scriptInitSetting : _config.getGlobalScriptInitSettings()) {
            _appScriptLocationMap.setScriptInitSetting(null, scriptInitSetting);
        }
        if(_config.getAppSettings() != null) {
            for(ScriptAppSetting appSetting : _config.getAppSettings()) {
                String app = appSetting.getApp();
                
                for(ScriptInitSetting scriptInitSetting : appSetting.getScriptInitSettings()) {
                    _appScriptLocationMap.setScriptInitSetting(app, scriptInitSetting);
                }
            }
        }
        
    }
    
    private FileFilter _scriptFileFilter = new FileFilter() {
        
        @Override
        public boolean accept(File file) {
            if(file.isHidden() || file.isDirectory()) {
                return false;
            }
            
            if(_scriptFileExtFilterNames.length > 0) {
                boolean matched = false;
                for(String filterExtName : _scriptFileExtFilterNames) {
                    matched = file.getName().endsWith(filterExtName);
                    if(matched) {
                        break;
                    }
                }
                if(!matched) {
                    return false;
                }
            }
            
            return true;
        }
    };
    
    private void initLoadScriptFiles() {
        for(IScriptSourceWatcher sourceWatcher : _scriptWatchers) {
            List<InitLoadScriptEntry> initLoadEntries = getInitLoadScriptFiles();
            try {
                sourceWatcher.onInitLoadScriptSource(initLoadEntries);
            } finally {
                for(InitLoadScriptEntry entry : initLoadEntries) {
                    try {
                        if(entry.getConfig() != null) {
                            entry.getConfig().close();
                        }
                    } catch (Throwable e) {
                        logger.error(null, e);
                    }
                }
            }
        }
    }
    
    private List<InitLoadScriptEntry> getInitLoadScriptFiles() {
        List<InitLoadScriptEntry> initLoadEntries = new ArrayList<>();
        
        //global script ------
        {
            List<File> fileList = new ArrayList<>();
            listFilesRecursively(fileList, _globalSourceDir, _scriptFileFilter);
            
            //load files defined in ScriptInitSettings 
            for(ScriptInitSetting initSetting : _config.getGlobalScriptInitSettings()) {
                File file = removeFile(fileList, initSetting.getScriptName());
                if(file == null) {
                    logger.error("Global script file not exits. scriptName:" + initSetting.getScriptName());
                    continue;
                }
                if(!file.exists()) {
                    logger.error("Global script file not exits. file:" + file.getAbsolutePath());
                    continue;
                }
                
                Reader config = null;
                try {
                    //handleDirWatchEvent(StandardWatchEventKinds.ENTRY_CREATE, file);
                    
                    config = _configLocationResolver.resolveConfigLocation(initSetting.getConfigLocation());
                    
                    logger.info("Global script file scanned. script:" + file.getAbsolutePath());                    
                    initLoadEntries.add(new InitLoadScriptEntry(
                            null, 
                            new TextFile(file, DEFAULT_CHARSET), 
                            config
                            ));
                } catch (Throwable e) {
                    try {
                        if(config != null) {
                            config.close();
                        }
                    } catch (Throwable e1) {
                        logger.error(null, e1);
                    }
                    
                    logger.error("Error occurred in load global script. file:" + file.getAbsolutePath(), e);
                }
            }
            
            for(File file : fileList) {
                //handleDirWatchEvent(StandardWatchEventKinds.ENTRY_CREATE, file);
                try {
                    logger.info("Global script file scanned. script:" + file.getAbsolutePath());
                    initLoadEntries.add(new InitLoadScriptEntry(
                            null, 
                            new TextFile(file, DEFAULT_CHARSET), 
                            null
                            ));
                } catch (Throwable e) {
                    logger.error("Error occurred in load global script. file:" + file.getAbsolutePath(), e);
                }
            }
        }
        
        //app script ------
        if(_config.getAppSettings() != null) {
            for(ScriptAppSetting appSetting : _config.getAppSettings()) {
                String app = appSetting.getApp();
                
                try {
                    List<File> fileList = new ArrayList<>();
                    listFilesRecursively(fileList, new File(_appSourceDir, app), _scriptFileFilter);
                    
                    //load files defined in ScriptInitSettings 
                    for(ScriptInitSetting initSetting : appSetting.getScriptInitSettings()) {
                        File file = removeFile(fileList, initSetting.getScriptName());
                        if(file == null) {
                            logger.error("App script file not exits. scriptName:" + initSetting.getScriptName());
                            continue;
                        }
                        if(!file.exists()) {
                            logger.error("App script file not exits. file:" + file.getAbsolutePath());
                            continue;
                        }
                        
                        Reader config = null;
                        try {
                            //handleDirWatchEvent(StandardWatchEventKinds.ENTRY_CREATE, file);

                            config = _configLocationResolver.resolveConfigLocation(initSetting.getConfigLocation());
                            
                            logger.info("App script file scanned. script:" + file.getAbsolutePath());
                            initLoadEntries.add(new InitLoadScriptEntry(
                                    app, 
                                    new TextFile(file, DEFAULT_CHARSET), 
                                    config
                                    ));
                        } catch (Throwable e) {
                            try {
                                if(config != null) {
                                    config.close();
                                }
                            } catch (Throwable e1) {
                                logger.error(null, e1);
                            }
                            
                            logger.error("Error occurred in load app script. file:" + file.getAbsolutePath(), e);
                        }
                    }
                    
                    for(File file : fileList) {
                        try {
                            //handleDirWatchEvent(StandardWatchEventKinds.ENTRY_CREATE, file);
                            
                            logger.info("App script file scanned. script:" + file.getAbsolutePath());
                            initLoadEntries.add(new InitLoadScriptEntry(
                                    app, 
                                    new TextFile(file, DEFAULT_CHARSET), 
                                    null
                                    ));
                        } catch (Throwable e) {
                            logger.error("Error occurred in load app script. file:" + file.getAbsolutePath(), e);
                        }
                    }
                } catch (Throwable e) {
                    logger.error("Error occurred in load app script. app:" + app, e);
                }
            }
        }
        
        return initLoadEntries;
    }
    
    private static void listFilesRecursively(
            final List<File> result, final File dir, final FileFilter filter
            ) {
        File[] files = dir.listFiles(filter);
        if(files != null && files.length > 0) {
            result.addAll(Arrays.asList(files));
        }
        
        //sub dirs 
        File[] subDirs = dir.listFiles(new FileFilter() {
            
            @Override
            public boolean accept(File file) {
                return file.isDirectory() && file.exists();
            }
        });
        if(subDirs == null || subDirs.length == 0) {
            return;
        }
        
        for(File subDir : subDirs) {
            listFilesRecursively(result, subDir, filter);
        }
    }

    private static File removeFile(List<File> fileList, String fileName) {
        int size = fileList.size();
        for(int i = 0; i < size; i++) {
            File file = fileList.get(i);
            
            //logger.debug("removeFile() -- file:" + file.getAbsolutePath() + " matchName:" + fileName);
            if(file.getAbsolutePath().endsWith(fileName)) {
                return fileList.remove(i);
            }
        }
        
        return null;
    }
    
    private void handleDirWatchEvent(WatchEvent.Kind<?> eventKind, File file) {
        //ignore hidden files
        if(!_scriptFileFilter.accept(file)) {
            logger.info("handleEvent() file ignored. ->"
                    + " eventKind: " + eventKind
                    + " file: " + file.getAbsolutePath()
                    + " file.isHidden: " + file.isHidden()
                    + " file.isDir: " + file.isDirectory()
                    );
            return;
        }
        
        //handle event
        /*
        final String app = parseapp(file);
        final String scriptName = file.getName();
        */
        final ScriptLocation scriptLoc = parseapp(file);
        
        if(eventKind == StandardWatchEventKinds.ENTRY_CREATE
                || eventKind == StandardWatchEventKinds.ENTRY_MODIFY
                ) {
            final ScriptInitSetting scriptInitSetting = _appScriptLocationMap.getScriptInitSetting(scriptLoc._app, scriptLoc._scriptName); 
            for(IScriptSourceWatcher sourceWatcher : _scriptWatchers) {
                try {
                    Reader config = null;
                    if(scriptInitSetting != null 
                            && scriptInitSetting.getConfigLocation() != null
                            && scriptInitSetting.getConfigLocation().trim().length() != 0
                            ) {
                        config = _configLocationResolver.resolveConfigLocation(scriptInitSetting.getConfigLocation());
                    }            
                    try {
                        String serviceName = sourceWatcher.onScriptSourceUpdated(
                                scriptLoc._app, new TextFile(file, DEFAULT_CHARSET), config
                                );
                        if(serviceName != null && serviceName.length() > 0) {
                            _appScriptLocationMap.setServiceName(scriptLoc._app, scriptLoc._scriptName, serviceName);
                        }
                    } finally {
                        if(config != null) {
                            config.close();
                        }
                    }                        
                } catch (Throwable e) {
                    logger.error("scriptFile:" + file.getAbsolutePath(), e);
                }
            }
        } else if(eventKind == StandardWatchEventKinds.ENTRY_DELETE) {
            final String serviceName = _appScriptLocationMap.getServiceName(scriptLoc._app, scriptLoc._scriptName);
            
            if(serviceName != null && serviceName.length() != 0) {
                for(IScriptSourceWatcher sourceWatcher : _scriptWatchers) {
                    try {
                        sourceWatcher.onScriptSourceDeleted(scriptLoc._app, serviceName);
                    } catch (Throwable e) {
                        logger.error("scriptFile:" + file.getAbsolutePath(), e);
                    }
                }
            }
        } else {
            throw new RuntimeException("Unexpected event kind:" + eventKind);
        }
    }
    
    private class AppScriptLocationMap {
        //key:$app + '/' + scriptName ('$' if global)       value:ScriptInitSetting
        private Map<String, ScriptInitSetting> _scriptInitSettingMap = new ConcurrentHashMap<>();
        
        //key:$app + '/' + scriptName ('$' if global)       value:serviceName
        private Map<String, String> _scriptServiceNameMap = new ConcurrentHashMap<>();
        
        public void clear() {
            try {
                _scriptInitSettingMap.clear();
            } catch (Throwable e) {
                logger.error(null, e);
            }
            
            try {
                _scriptServiceNameMap.clear();
            } catch (Throwable e) {
                logger.error(null, e);
            }
        }
        
        public ScriptInitSetting getScriptInitSetting(
                String app, String scriptName
                ) {
            return _scriptInitSettingMap.get(
                    toAppScriptLocation(app, scriptName) 
                    );
        }

        public void setScriptInitSetting(
                String app, 
                ScriptInitSetting scriptInitSetting
                ) {
            _scriptInitSettingMap.put(
                    toAppScriptLocation(app, scriptInitSetting.getScriptName()), 
                    scriptInitSetting
                    );
        }
        
        public String getServiceName(String app, String scriptName) {
            return _scriptServiceNameMap.get(toAppScriptLocation(app, scriptName));
        }
        
        public void setServiceName(String app, String scriptName, String serviceName) {
            _scriptServiceNameMap.put(toAppScriptLocation(app, scriptName), serviceName);
        }
        
        private String toAppScriptLocation(String app, String scriptName) {
            if(scriptName.charAt(0) == '/') {
                throw new IllegalArgumentException("ScriptName must not statswith '/'"
                        + " app:" + app
                        + " scriptName:" + scriptName
                        );
            }
            
            if(app == null || app.length() == 0) {
                return APP_NAME_GLOBAL + "/" + scriptName;
            } else {
                return app + "/" + scriptName;
            }
        }
    }

    /**
     * 
     * @param file
     * @return null if global
     */
    private ScriptLocation parseapp(File file) {
        /*
        final File parent = file.getParentFile();
        
        if(parent.getAbsolutePath().equals(_globalSourceDir.getAbsolutePath())) {
            return null;
        } else {
            if(parent.getParentFile().getAbsolutePath().equals(_appSourceDir.getAbsolutePath())) {
                return parent.getName();
            } else {
                throw new RuntimeException("Invalid script file path: " + file.getAbsolutePath());
            }
        }
        */
        
        for(Entry<String, String> entry : _appSourceDirMap.entrySet()) {
            String app = entry.getKey();
            String appSrcDir = entry.getValue();
            
            if(file.getAbsolutePath().startsWith(appSrcDir)) {
                ScriptLocation scriptLoc = new ScriptLocation();
                scriptLoc._app = app;
                
                int begin = appSrcDir.length();
                if(appSrcDir.charAt(begin - 1) != '/') {
                    begin ++;
                }
                scriptLoc._scriptName = file.getAbsolutePath().substring(begin);
                return scriptLoc;
            }
        }
        
        if(file.getAbsolutePath().startsWith(getGlobalSourceDir().getAbsolutePath())) {
            ScriptLocation scriptLoc = new ScriptLocation();
            scriptLoc._app = null;
            
            int begin = getGlobalSourceDir().getAbsolutePath().length();
            if(getGlobalSourceDir().getAbsolutePath().charAt(begin - 1) != '/') {
                begin ++;
            }
            scriptLoc._scriptName = file.getAbsolutePath().substring(begin);
            return scriptLoc;
        } else {
            throw new RuntimeException("Invalid script file path: " + file.getAbsolutePath());
        }
    }
    
    private File getGlobalSourceDir() {
        return new File(_config.getGlobalSourceDir());
    }
    
    private File getAppSourceDir(String app) {
        return new File(_config.getAppSourceDir(), app);
    }
        
    private static String readText(Reader reader) throws IOException {
        StringBuilder str = new StringBuilder();

        char[] cBuf = new char[256];
        final int bufLen = cBuf.length;
        int readLen;
        while(true) {
            readLen = reader.read(cBuf, 0, bufLen);
            
            if(readLen < 0) {
                break;
            }
            
            if(readLen > 0) {
                str.append(cBuf, 0, readLen);
            }
        }
        
        return str.toString();
    }

    private static class ScriptLocation {
        public String _app;
        public String _scriptName;        
    }

}
