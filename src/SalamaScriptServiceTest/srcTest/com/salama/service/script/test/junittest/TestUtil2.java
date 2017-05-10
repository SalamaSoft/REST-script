package com.salama.service.script.test.junittest;

import java.util.ArrayList;
import java.util.List;

public class TestUtil2 {
    
    public static long utc() {
        return System.currentTimeMillis();
    }
    
    public static List<String> testStrList() {
        List<String> list = new ArrayList<String>();
        
        list.add("e0");
        list.add("e1");
        list.add("e2");
        list.add("e3");
        list.add("e4");
        
        return list;
    }

}
