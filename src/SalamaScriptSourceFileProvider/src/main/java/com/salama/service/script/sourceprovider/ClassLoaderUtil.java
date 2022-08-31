package com.salama.service.script.sourceprovider;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLStreamHandlerFactory;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class ClassLoaderUtil {
    private final static Log logger = LogFactory.getLog(ClassLoaderUtil.class);
    
    private final static FileFilter _jarFileFilter = new FileFilter() {
        
        @Override
        public boolean accept(File file) {
            return (!file.isDirectory() 
                    && file.getName().toLowerCase().endsWith(".jar"));
        }
    };
    
    
    private static class MyClassLoader extends URLClassLoader {
        
        public MyClassLoader(URL[] urls) {
            super(urls);
        }
        
        public MyClassLoader(URL[] urls, ClassLoader parent) {
            super(urls, parent);
        }

        public MyClassLoader(URL[] urls, ClassLoader parent, URLStreamHandlerFactory factory) {
            super(urls, parent, factory);
        }
        
        @Override
        protected Class<?> findClass(String name) throws ClassNotFoundException {
            final Class<?> cls = super.findClass(name);
            
            logger.info("findClass() -> " + cls);
            return cls;
        }
    }
    
    public static boolean addLibraryPath(File dir) throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException {
        final String newLibPath = dir.getAbsolutePath();
        
        Field field = ClassLoader.class.getDeclaredField("usr_paths");
        field.setAccessible(true);
        
        String[] paths = (String[]) field.get(null);
        for(String path : paths) {
            if(path.equals(newLibPath)) {
                return false;
            }
        }
        
        //copy and append
        String[] newPaths = new String[paths.length + 1];
        System.arraycopy(paths, 0, newPaths, 0, paths.length);
        newPaths[paths.length] = newLibPath;
        
        field.set(null, newPaths);
        return true;
    }

    public static void loadJarsIntoUrlClassLoader(URLClassLoader loader, File dir) throws IOException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException {
        File[] jarFiles = dir.listFiles(_jarFileFilter);
        if(jarFiles == null || jarFiles.length == 0) {
            return;
        }
        
        Method method = URLClassLoader.class.getDeclaredMethod("addURL", new Class[] {URL.class});
        method.setAccessible(true);

        for(int i = 0; i < jarFiles.length; i++) {
            URL jarUrl = jarFiles[i].toURI().toURL();
            
            method.invoke(loader, jarUrl);
            logger.info("loadJarsIntoUrlClassLoader() jar -> " + jarUrl);
        }
    }
    
    public static ClassLoader createUrlClassLoaderWithJarsDir(ClassLoader parentLoader, File dir) throws IOException {
        File[] jarFiles = dir.listFiles(_jarFileFilter);
        
        URL[] urls = new URL[jarFiles.length];
        for(int i = 0; i < jarFiles.length; i++) {
            URL jarUrl = jarFiles[i].toURI().toURL(); 
            urls[i] = jarUrl;
            
            //logger.info("loadJarsInDir() -> " + jarUrl);
        }
        
        //create new ClassLoader
        MyClassLoader classLoader = new MyClassLoader(urls, parentLoader);

        //scan all classes in jars
        for(File file : jarFiles) {
            logger.info("loadJarsInDir() jar -> " + file.getAbsolutePath());

            try {
                JarFile jarFile = new JarFile(file);
                try {
                    loadJarFile(classLoader, jarFile);
                } finally {
                    jarFile.close();
                }
            } catch (Throwable e) {
                logger.error("Error in loading jar -> " + file.getAbsolutePath(), e);
            }
        }
        
        return classLoader;
    }
    
    private static void loadJarFile(ClassLoader classLoader, JarFile jarFile) {
        Enumeration<JarEntry> enumEntry = jarFile.entries();
        while(enumEntry.hasMoreElements()) {
            JarEntry entry = enumEntry.nextElement();
            
            try {
                if(!entry.getName().toLowerCase().endsWith(".class")) {
                    continue;
                }
                
                String className = getClassName(entry);
                final Class<?> cls = classLoader.loadClass(className);
                //logger.info("loadJarsInDir() scanned class -> " + cls);
            } catch (ClassNotFoundException e) {
                logger.error("loadJarsInDir() error in loading -> " + entry.getName(), e);
            }
        }
    }
    
    private static String getClassName(JarEntry entry) {
        //e.g. "org/apache/commons/codec/Encoder.class"
        String entryName = entry.getName();
        
        String classFileName = entryName.replace('/', '.');
        int lastDotPos = classFileName.lastIndexOf('.');
        String className = classFileName.substring(0, lastDotPos);
        
        return className;
    }
}
