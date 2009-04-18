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


import junit.framework.TestCase;

import org.apache.felix.ipojo.metadata.Element;

/**
 * Tests about {@link InstantiatedService}.
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class InstantiatedServiceTest extends TestCase {
    
//    <comp:composite name="composite.bar.1" architecture="true">
//    <subservice action="instantiate" specification="org.apache.felix.ipojo.test.composite.service.BarService"/>
//</comp:composite>
    
    /**
     * Simple test.
     */
    public void testSimple() {
        InstantiatedService svc = new InstantiatedService()
            .setSpecification("org.apache.felix.ipojo.test.composite.service.BarService");
        Element elem = svc.getElement();
        String spec = elem.getAttribute("specification");
        String action = elem.getAttribute("action");

        assertEquals("spec" , "org.apache.felix.ipojo.test.composite.service.BarService", spec);
        assertEquals("action" , "instantiate", action);
    }
    
    /**
     * Malformed instantiated service.
     */
    public void testBad() {
        InstantiatedService svc = new InstantiatedService()
            //.setSpecification("org.apache.felix.ipojo.test.composite.service.BarService") NO SPEC
            ;
        try {
            svc.getElement();
            fail("Invalid element accepted");
        } catch (IllegalStateException e) {
            // OK
        }
    }
    
    
    
    
//
//<comp:composite name="composite.bar.2" architecture="true">
//    <subservice action="instantiate" specification="org.apache.felix.ipojo.test.composite.service.BarService" aggregate="true"/>
//</comp:composite>
    
    /**
     * Aggregate.
     */
    public void testAggregate() {
        InstantiatedService svc = new InstantiatedService()
            .setSpecification("org.apache.felix.ipojo.test.composite.service.BarService")
            .setAggregate(true);
        Element elem = svc.getElement();
        String spec = elem.getAttribute("specification");
        String action = elem.getAttribute("action");
        String agg = elem.getAttribute("aggregate");


        assertEquals("spec" , "org.apache.felix.ipojo.test.composite.service.BarService", spec);
        assertEquals("action" , "instantiate", action);
        assertEquals("aggregate" , "true", agg);

    }
    
//
//<comp:composite name="composite.bar.3" architecture="true">
//    <subservice action="instantiate" specification="org.apache.felix.ipojo.test.composite.service.BarService" optional="true"/>
//</comp:composite>
    /**
     * Optional.
     */
    public void testOptional() {
        InstantiatedService svc = new InstantiatedService()
            .setSpecification("org.apache.felix.ipojo.test.composite.service.BarService")
            .setOptional(true);
        Element elem = svc.getElement();
        String spec = elem.getAttribute("specification");
        String action = elem.getAttribute("action");
        String agg = elem.getAttribute("aggregate");
        String opt = elem.getAttribute("optional");

        assertEquals("spec" , "org.apache.felix.ipojo.test.composite.service.BarService", spec);
        assertEquals("action" , "instantiate", action);
        assertNull("aggregate" , agg);
        assertEquals("optional" , "true", opt);
    }
    
//
//<comp:composite name="composite.bar.4" architecture="true">
//    <subservice action="instantiate" specification="org.apache.felix.ipojo.test.composite.service.FooService" aggregate="true" optional="true"/>
//</comp:composite>
    /**
     * Aggregate and optional.
     */
    public void testOptionalAndAggregate() {
        InstantiatedService svc = new InstantiatedService()
            .setSpecification("org.apache.felix.ipojo.test.composite.service.FooService")
            .setOptional(true)
            .setAggregate(true);
        Element elem = svc.getElement();
        String spec = elem.getAttribute("specification");
        String action = elem.getAttribute("action");
        String agg = elem.getAttribute("aggregate");
        String opt = elem.getAttribute("optional");

        assertEquals("spec" , "org.apache.felix.ipojo.test.composite.service.FooService", spec);
        assertEquals("action" , "instantiate", action);
        assertEquals("aggregate" , "true", agg);
        assertEquals("optional" , "true", opt);
    }
    
//
//<comp:composite name="composite.bar.5-accept" architecture="true">
//    <subservice action="instantiate" specification="org.apache.felix.ipojo.test.composite.service.FooService">
//        <property name="boolean" value="true"/>
//        <property name="string" value="foo"/>
//        <property name="strAprop" value="{foo, bar, baz}"/>
//        <property name="int" value="5"/>
//    </subservice>
//</comp:composite>
    /**
     * Instance configuration.
     */
    public void testWithConfiguration() {
        InstantiatedService svc = new InstantiatedService()
            .setSpecification("org.apache.felix.ipojo.test.composite.service.FooService")
            .addProperty("boolean", "true")
            .addProperty("string", "foo")
            .addProperty("stringAprop", new String[] {"foo", "bar", "baz" })
            .addProperty("int", "5");
        Element elem = svc.getElement();
        String spec = elem.getAttribute("specification");
        String action = elem.getAttribute("action");
        String agg = elem.getAttribute("aggregate");
        String opt = elem.getAttribute("optional");
       
        assertEquals("spec" , "org.apache.felix.ipojo.test.composite.service.FooService", spec);
        assertEquals("action" , "instantiate", action);
        assertNull("aggregate" , agg);
        assertNull("optional" , opt);
        
        Element[] props = elem.getElements("property");
        assertEquals("Number of properties", 4, props.length);
        String n0 = props[0].getAttribute("name");
        String v0 = props[0].getAttribute("value");
        assertEquals("n0" , "boolean", n0);
        assertEquals("v0" , "true", v0);
        
        String n1 = props[1].getAttribute("name");
        String v1 = props[1].getAttribute("value");
        assertEquals("n1" , "string", n1);
        assertEquals("v1" , "foo", v1);
        
        String n2 = props[2].getAttribute("name");
        String v2 = props[2].getAttribute("value");
        String t2 = props[2].getAttribute("type");
        Element[] sub = props[2].getElements();
        assertEquals("Number of sub-properties", 3, sub.length);
        assertEquals("n2", "stringAprop", n2);
        assertNull("v2", v2);
        assertEquals("t2", "array", t2);

        String n20 = sub[0].getAttribute("name");
        String v20 = sub[0].getAttribute("value");
        assertNull("n20" , n20);
        assertEquals("v20" , "foo", v20);
        String n21 = sub[1].getAttribute("name");
        String v21 = sub[1].getAttribute("value");
        assertNull("n21" , n21);
        assertEquals("v21" , "bar", v21);
        String n22 = sub[2].getAttribute("name");
        String v22 = sub[2].getAttribute("value");
        assertNull("n22" , n22);
        assertEquals("v22" , "baz", v22);
        
        String n3 = props[3].getAttribute("name");
        String v3 = props[3].getAttribute("value");
        assertEquals("n3" , "int", n3);
        assertEquals("v3" , "5", v3);
    }


}
