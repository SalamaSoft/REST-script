package com.salama.service.script.sourceprovider.junittest;

import java.lang.reflect.Field;

import org.junit.Test;

public class TestClassLoader {

    @Test
    public void test2() {
        try {
            Field field = ClassLoader.class.getDeclaredField("usr_paths");
            field.setAccessible(true);
            
            String[] usr_paths = (String[]) field.get(ClassLoader.class);
            
            for(int i = 0; i < usr_paths.length; i++) {
                String path = usr_paths[i];
                System.out.println("usr_paths[" + i + "]:" + path);
            }
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }
    
    public void test1() {
        try {
             ClassLoader cl1 = TestClassLoader.class.getClassLoader();
             ClassLoader cl2 = Thread.currentThread().getContextClassLoader();
             
             System.out.println("classLoader1:" + cl1);
             System.out.println("classLoader2:" + cl2);
             
             ClassLoader clTmp = cl2.getParent();
             while(clTmp != null) {
                 System.out.println("parentClassLoader:" + clTmp);
                 
                 clTmp = clTmp.getParent();
             }
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }
}
