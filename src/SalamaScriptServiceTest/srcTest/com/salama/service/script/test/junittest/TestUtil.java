package com.salama.service.script.test.junittest;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TestUtil {

    public String ymdHms() {
        return (new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")).format(new Date()); 
    }
    
    public long utc() {
        return System.currentTimeMillis();
    }
    
    public Map<String, String> testPlainMap() {
        Map<String, String> map = new HashMap<String, String>();
        
        map.put("k1", "v1");
        map.put("k2", "v2");
        map.put("k3", "v3");
        
        return map;
    }
    
    public Map<String, Object> testComplexMap() {
        Map<String, Object> map = new HashMap<String, Object>();
        
        map.put("k1", "v1");
        map.put("k2", "v2");
        map.put("m1", testPlainMap());
        ((Map<String, Object>) map.get("m1")).put("m2", testPlainMap());
        
        return map;
    }
    
    public String[] testStrArray() {
        return new String[] {"e0", "e1", "e2", "e3"};
    }
    
    public List<String> testStrList() {
        List<String> list = new ArrayList<String>();
        
        list.add("e0");
        list.add("e1");
        list.add("e2");
        list.add("e3");
        list.add("e4");
        
        return list;
    }
    
}
