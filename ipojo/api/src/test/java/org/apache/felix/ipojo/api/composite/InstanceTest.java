/* 
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.felix.ipojo.api.composite;

import java.util.ArrayList;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Vector;

import junit.framework.TestCase;

import org.apache.felix.ipojo.metadata.Element;

/**
 * Tests about {@link Instance}.
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class InstanceTest extends TestCase {
    
    /**
     * One simple instance.
     */
    public void testJustComponent() {
        Instance inst = new Instance("mycmp");
        Element elem = inst.getElement();
        String cmp = elem.getAttribute("component");
        assertEquals("Check component attribute", "mycmp", cmp);
    }
    
    /**
     * Sting property.
     */
    public void testStringProp() {
        Instance inst = new Instance("mycmp");
        inst.addProperty("p1", "v1");
        Element elem = inst.getElement();
        String cmp = elem.getAttribute("component");
        Element[] elems = elem.getElements();
        assertEquals("Check component attribute", "mycmp", cmp);
        assertNotNull("Check properties", elems);
        assertEquals("Check properties count", 1, elems.length);
        String n = elems[0].getAttribute("name");
        String v = elems[0].getAttribute("value");
        String t = elems[0].getAttribute("type");


        assertEquals("Check property 0 - name", "p1", n);
        assertEquals("Check property 0 - value", "v1", v);
        assertNull("Check property 0 - type", t);

    }
    
    /**
     * Several properties.
     */
    public void testStringProps() {
        Instance inst = new Instance("mycmp");
        inst.addProperty("p1", "v1");
        inst.addProperty("p2", "v2");
        Element elem = inst.getElement();
        String cmp = elem.getAttribute("component");
        Element[] elems = elem.getElements();
        assertEquals("Check component attribute", "mycmp", cmp);
        assertNotNull("Check properties", elems);
        assertEquals("Check properties count", 2, elems.length);
        String n = elems[0].getAttribute("name");
        String v = elems[0].getAttribute("value");
        assertEquals("Check property 0 - name", "p1", n);
        assertEquals("Check property 0 - value", "v1", v);
        n = elems[1].getAttribute("name");
        v = elems[1].getAttribute("value");
        assertEquals("Check property 1 - name", "p2", n);
        assertEquals("Check property 1 - value", "v2", v);
    }
    
    /**
     * List property.
     */
    public void testListProp() {
        Instance inst = new Instance("mycmp");
        List list = new ArrayList();
        list.add("a");
        list.add("a");
        list.add("a");
        
        inst.addProperty("p1", list);
        Element elem = inst.getElement();
        String cmp = elem.getAttribute("component");
        Element[] elems = elem.getElements();
        assertEquals("Check component attribute", "mycmp", cmp);
        assertNotNull("Check properties", elems);
        assertEquals("Check properties count", 1, elems.length);
        String n = elems[0].getAttribute("name");
        String v = elems[0].getAttribute("value");
        String t = elems[0].getAttribute("type");

        assertEquals("Check property 0 - name", "p1", n);
        assertNull("Check property 0 - value", v);
        assertEquals("Check property 0 - type", "list", t);

        
        Element[] subs = elems[0].getElements();
        assertEquals("Check the number of sub-elements", 3, subs.length);
        for (int i = 0; i < subs.length; i++) {
            Element a = subs[i];
            assertEquals("Check the value of " + i, "a", a.getAttribute("value"));
            assertNull("Check the name of " + i, a.getAttribute("name"));

        }
    }
    
    /**
     * Array property.
     */
    public void testArrayProp() {
        Instance inst = new Instance("mycmp");
        String[] list = new String[] {"a", "a", "a"};
        
        inst.addProperty("p1", list);
        Element elem = inst.getElement();
        String cmp = elem.getAttribute("component");
        Element[] elems = elem.getElements();
        assertEquals("Check component attribute", "mycmp", cmp);
        assertNotNull("Check properties", elems);
        assertEquals("Check properties count", 1, elems.length);
        String n = elems[0].getAttribute("name");
        String v = elems[0].getAttribute("value");
        String t = elems[0].getAttribute("type");

        assertEquals("Check property 0 - name", "p1", n);
        assertNull("Check property 0 - value", v);
        assertEquals("Check property 0 - type", "array", t);

        
        Element[] subs = elems[0].getElements();
        assertEquals("Check the number of sub-elements", 3, subs.length);
        for (int i = 0; i < subs.length; i++) {
            Element a = subs[i];
            assertEquals("Check the value of " + i, "a", a.getAttribute("value"));
            assertNull("Check the name of " + i, a.getAttribute("name"));

        }
    }
    
    /**
     * Vector property.
     */
    public void testVectorProp() {
        Instance inst = new Instance("mycmp");
        Vector list = new Vector();
        list.add("a");
        list.add("a");
        list.add("a");
        
        inst.addProperty("p1", list);
        Element elem = inst.getElement();
        String cmp = elem.getAttribute("component");
        Element[] elems = elem.getElements();
        assertEquals("Check component attribute", "mycmp", cmp);
        assertNotNull("Check properties", elems);
        assertEquals("Check properties count", 1, elems.length);
        String n = elems[0].getAttribute("name");
        String v = elems[0].getAttribute("value");
        String t = elems[0].getAttribute("type");
        
        
        assertEquals("Check property 0 - name", "p1", n);
        assertNull("Check property 0 - value", v);
        assertEquals("Check property 0 - type", "vector", t);

        
        Element[] subs = elems[0].getElements();
        assertEquals("Check the number of sub-elements", 3, subs.length);
        for (int i = 0; i < subs.length; i++) {
            Element a = subs[i];
            assertEquals("Check the value of " + i, "a", a.getAttribute("value"));
            assertNull("Check the name of " + i, a.getAttribute("name"));

        }
    }
    
    /**
     * Map property.
     */
    public void testMapProp() {
        Instance inst = new Instance("mycmp");
        Map map = new HashMap();
        map.put("p1", "b");
        map.put("p2", "b");
        map.put("p3", "b");
        
        inst.addProperty("p1", map);
        Element elem = inst.getElement();
        String cmp = elem.getAttribute("component");
        Element[] elems = elem.getElements();
        assertEquals("Check component attribute", "mycmp", cmp);
        assertNotNull("Check properties", elems);
        assertEquals("Check properties count", 1, elems.length);
        String n = elems[0].getAttribute("name");
        String v = elems[0].getAttribute("value");
        String t = elems[0].getAttribute("type");

        
        assertEquals("Check property 0 - name", "p1", n);
        assertNull("Check property 0 - value", v);
        assertEquals("Check property 0 - type", "map", t);

        Element[] subs = elems[0].getElements();
        assertEquals("Check the number of sub-elements", 3, subs.length);
        for (int i = 0; i < subs.length; i++) {
            Element a = subs[i];
            assertEquals("Check the value of " + i, "b", a.getAttribute("value"));
            assertNotNull("Check the name of " + i, a.getAttribute("name"));

        }
    }
    
    /**
     * Dictionary property.
     */
    public void testDictProp() {
        Instance inst = new Instance("mycmp");
        Dictionary map = new Properties();
        map.put("p1", "b");
        map.put("p2", "b");
        map.put("p3", "b");
        
        inst.addProperty("p1", map);
        Element elem = inst.getElement();
        String cmp = elem.getAttribute("component");
        Element[] elems = elem.getElements();
        assertEquals("Check component attribute", "mycmp", cmp);
        assertNotNull("Check properties", elems);
        assertEquals("Check properties count", 1, elems.length);
        String n = elems[0].getAttribute("name");
        String v = elems[0].getAttribute("value");
        String t = elems[0].getAttribute("type");

        
        assertEquals("Check property 0 - name", "p1", n);
        assertNull("Check property 0 - value", v);
        assertEquals("Check property 0 - type", "dictionary", t);

        
        Element[] subs = elems[0].getElements();
        assertEquals("Check the number of sub-elements", 3, subs.length);
        for (int i = 0; i < subs.length; i++) {
            Element a = subs[i];
            assertEquals("Check the value of " + i, "b", a.getAttribute("value"));
            assertNotNull("Check the name of " + i, a.getAttribute("name"));

        }
    }

}
