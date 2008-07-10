package org.apache.felix.ipojo.test.scenarios.configuration;

import java.util.Dictionary;
import java.util.List;
import java.util.Map;

import org.apache.felix.ipojo.junit4osgi.OSGiTestCase;
import org.apache.felix.ipojo.test.scenarios.configuration.service.CheckService;
import org.apache.felix.ipojo.test.scenarios.util.Utils;
import org.osgi.framework.ServiceReference;

public class TestComplexProperties extends OSGiTestCase {
    
    private ServiceReference m_ref;
    private CheckService m_check;
    
    public void setUp() {
       m_ref = Utils.getServiceReferenceByName(context, CheckService.class.getName(), "complex");
       assertNotNull("Complex service availability", m_ref);
       m_check = (CheckService) context.getService(m_ref);
    }
    
    public void tearDown() {
        m_check = null;
        context.ungetService(m_ref);
    }
    
    public void testArray() {
        String[] array = (String[]) m_check.getProps().get("array");
        assertEquals("Array size", 2, array.length);
        assertEquals("Array[0]", "a", array[0]);
        assertEquals("Array[1]", "b", array[1]);
    }
    
    public void testList() {
        List list = (List) m_check.getProps().get("list");
        assertEquals("List size", 2, list.size());
        assertEquals("List[0]", "a", list.get(0));
        assertEquals("List[1]", "b", list.get(1));
    }
    
    public void testMap() {
        Map map = (Map) m_check.getProps().get("map");
        assertEquals("Map size", 2, map.size());
        assertEquals("Map[a]", "a", map.get("a"));
        assertEquals("Map[b]", "b", map.get("b"));
    }
    
    public void testDictionary() {
        Dictionary dict = (Dictionary) m_check.getProps().get("dict");
        assertEquals("Map size", 2, dict.size());
        assertEquals("Map[a]", "a", dict.get("a"));
        assertEquals("Map[b]", "b", dict.get("b"));
    }
    
    public void testComplexArray() {
        Object[] array = (Object[]) m_check.getProps().get("complex-array");
        assertEquals("Array size", 2, array.length);
        assertTrue("Array[0] type", array[0] instanceof List);
        assertTrue("Array[1] type", array[1] instanceof List);
        List list = (List) array[0];
        assertEquals("List size", 2, list.size());
        assertEquals("List[0]", "a", list.get(0));
        assertEquals("List[1]", "b", list.get(1));
        list = (List) array[1];
        assertEquals("List size - 2", 2, list.size());
        assertEquals("List[0] - 2", "c", list.get(0));
        assertEquals("List[1] - 2", "d", list.get(1));
    }
    
    public void testComplexList() {
        List list = (List) m_check.getProps().get("complex-list");
        assertEquals("List size", 2, list.size());
        assertTrue("List[0] type", list.get(0) instanceof List);
        assertTrue("List[1] type", list.get(1) instanceof List);
        List list1 = (List) list.get(0);
        assertEquals("List size - 1", 2, list1.size());
        assertEquals("List[0] - 1", "a", list1.get(0));
        assertEquals("List[1] - 1", "b", list1.get(1));
        list1 = (List) list.get(1);
        assertEquals("List size - 2", 2, list1.size());
        assertEquals("List[0] - 2", "c", list1.get(0));
        assertEquals("List[1] - 2", "d", list1.get(1));
    }
    
    public void testComplexMap() {
        Map map = (Map) m_check.getProps().get("complex-map");
        assertEquals("List size", 2, map.size());
        assertTrue("List[0] type", map.get("a") instanceof List);
        assertTrue("List[1] type", map.get("b") instanceof List);
        List list = (List) map.get("a");
        assertEquals("List size - 1", 2, list.size());
        assertEquals("List[0] - 1", "a", list.get(0));
        assertEquals("List[1] - 1", "b", list.get(1));
        list = (List) map.get("b");
        assertEquals("List size - 2", 2, list.size());
        assertEquals("List[0] - 2", "c", list.get(0));
        assertEquals("List[1] - 2", "d", list.get(1));
    }
    
    public void testServiceArray() {
        String[] array = (String[]) m_ref.getProperty("array");
        assertEquals("Array size", 2, array.length);
        assertEquals("Array[0]", "a", array[0]);
        assertEquals("Array[1]", "b", array[1]);
    }
    
    public void testServiceList() {
        List list = (List) m_ref.getProperty("list");
        assertEquals("List size", 2, list.size());
        assertEquals("List[0]", "a", list.get(0));
        assertEquals("List[1]", "b", list.get(1));
    }
    
    public void testServiceMap() {
        Map map = (Map) m_ref.getProperty("map");
        assertEquals("Map size", 2, map.size());
        assertEquals("Map[a]", "a", map.get("a"));
        assertEquals("Map[b]", "b", map.get("b"));
    }
    
    public void testServiceDictionary() {
        Dictionary dict = (Dictionary) m_ref.getProperty("dict");
        assertEquals("Map size", 2, dict.size());
        assertEquals("Map[a]", "a", dict.get("a"));
        assertEquals("Map[b]", "b", dict.get("b"));
    }
    
    public void testServiceComplexArray() {
        Object[] array = (Object[]) m_ref.getProperty("complex-array");
        assertEquals("Array size", 2, array.length);
        assertTrue("Array[0] type", array[0] instanceof List);
        assertTrue("Array[1] type", array[1] instanceof List);
        List list = (List) array[0];
        assertEquals("List size", 2, list.size());
        assertEquals("List[0]", "a", list.get(0));
        assertEquals("List[1]", "b", list.get(1));
        list = (List) array[1];
        assertEquals("List size - 2", 2, list.size());
        assertEquals("List[0] - 2", "c", list.get(0));
        assertEquals("List[1] - 2", "d", list.get(1));
    }
    
    public void testServiceComplexList() {
        List list = (List) m_ref.getProperty("complex-list");
        assertEquals("List size", 2, list.size());
        assertTrue("List[0] type", list.get(0) instanceof List);
        assertTrue("List[1] type", list.get(1) instanceof List);
        List list1 = (List) list.get(0);
        assertEquals("List size - 1", 2, list1.size());
        assertEquals("List[0] - 1", "a", list1.get(0));
        assertEquals("List[1] - 1", "b", list1.get(1));
        list1 = (List) list.get(1);
        assertEquals("List size - 2", 2, list1.size());
        assertEquals("List[0] - 2", "c", list1.get(0));
        assertEquals("List[1] - 2", "d", list1.get(1));
    }
    
    public void testServiceComplexMap() {
        Map map = (Map) m_ref.getProperty("complex-map");
        assertEquals("List size", 2, map.size());
        assertTrue("List[0] type", map.get("a") instanceof List);
        assertTrue("List[1] type", map.get("b") instanceof List);
        List list = (List) map.get("a");
        assertEquals("List size - 1", 2, list.size());
        assertEquals("List[0] - 1", "a", list.get(0));
        assertEquals("List[1] - 1", "b", list.get(1));
        list = (List) map.get("b");
        assertEquals("List size - 2", 2, list.size());
        assertEquals("List[0] - 2", "c", list.get(0));
        assertEquals("List[1] - 2", "d", list.get(1));
    }
    
    public void testServiceEmptyArray() {
        String[] array = (String[]) m_ref.getProperty("empty-array");
        assertEquals("Array size", 0, array.length);
    }
    
    public void testServiceEmptyList() {
        List list = (List) m_ref.getProperty("empty-list");
        assertEquals("List size", 0, list.size());
    }
    
    public void testServiceEmptyMap() {
        Map map = (Map) m_ref.getProperty("empty-map");
        assertEquals("Map size", 0, map.size());
    }

}
