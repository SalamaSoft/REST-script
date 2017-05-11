package com.salama.service.script.junittest;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;

public class TestJsonAndXml {

    public void testXml_1() {
        
    }
    
    @Test
    public void testJson_3() {
        testJson(makeTestData3());
    }
    
    public void testJson_2() {
        testJson(makeTestData2());
    }
    
    public void testJson_1() {
        testJson(makeComplexMap());
    }
    
    private static void testJson(Object obj) {
        String jsonStr = JSON.toJSONString(obj, true);
        System.out.println("json.tostr -> \n" + jsonStr);

        Object obj2 = JSON.parseObject(jsonStr, obj.getClass());
        System.out.println("json.parse -> \n" + obj2);
    }
    
    private static Map<String, Object> makeComplexMap() {
        Map<String, Object> map = makePlainMap();
        map.put("k4", makePlainMap());
        ((Map<String, Object>) map.get("k4")).put("k5", makePlainMap());
        
        return map;
    }
    
    private static Map<String, Object> makePlainMap() {
        Map<String, Object> map = new HashMap<>();
        
        map.put("k1", "v1");
        map.put("k2", "v2");
        map.put("k3", "v3");
        
        return map;
    }

    private static TestData3 makeTestData3() {
        TestData3 data = new TestData3();
        
        data.setK3("kv3");
        data.setData1(makeTestData1());
        
        Map<String, Object> map = new HashMap<>();
        map.put("m1", "mv1");
        map.put("m2", 199.0);
        map.put("d1", makeTestData1());
        
        data.setValMap(map);
        
        return data;
    }

    private static TestData2 makeTestData2() {
        TestData2 data2 = new TestData2();
        data2.setK3("v3");
        
        data2.setData1(makeTestData1());
        
        return data2;
    }
    
    private static TestData1 makeTestData1() {
        TestData1 data1 = new TestData1();
        data1.setK1("v1");
        data1.setK2("v2");
        
        return data1;
    }
    
    private static class TestData3 {
        private String _k3;
        
        private TestData1 _data1;
        
        private Map<String, Object> _valMap;

        public String getK3() {
            return _k3;
        }

        public void setK3(String k3) {
            _k3 = k3;
        }

        public TestData1 getData1() {
            return _data1;
        }

        public void setData1(TestData1 data1) {
            _data1 = data1;
        }

        public Map<String, Object> getValMap() {
            return _valMap;
        }

        public void setValMap(Map<String, Object> valMap) {
            _valMap = valMap;
        }
        
        
    } 

    private static class TestData2 {
        private String _k3;
        
        private TestData1 _data1;

        public String getK3() {
            return _k3;
        }

        public void setK3(String k3) {
            _k3 = k3;
        }

        public TestData1 getData1() {
            return _data1;
        }

        public void setData1(TestData1 data1) {
            _data1 = data1;
        }
        
        
    } 
    
    private static class TestData1 {
        private String _k1;
        
        private String _k2;

        public String getK1() {
            return _k1;
        }

        public void setK1(String k1) {
            _k1 = k1;
        }

        public String getK2() {
            return _k2;
        }

        public void setK2(String k2) {
            _k2 = k2;
        }
        
        
    }
}
