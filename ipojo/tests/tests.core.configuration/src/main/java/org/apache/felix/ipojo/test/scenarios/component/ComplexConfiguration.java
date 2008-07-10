package org.apache.felix.ipojo.test.scenarios.component;

import java.util.Dictionary;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.felix.ipojo.test.scenarios.configuration.service.CheckService;

public class ComplexConfiguration implements CheckService {
    
    private List m_list;
    private Map m_map;
    private Dictionary m_dict;
    private String[] m_array;
    
    private List m_complexList;
    private Map m_complexMap;
    private Object[] m_complexArray;

    public boolean check() {
        return true;
    }
    

    public Properties getProps() {
        Properties props = new Properties();
        props.put("list", m_list);
        props.put("map", m_map);
        props.put("dict", m_dict);
        props.put("array", m_array);
        props.put("complex-list", m_complexList);
        props.put("complex-map", m_complexMap);
        props.put("complex-array", m_complexArray);
        return props;
    }

}
