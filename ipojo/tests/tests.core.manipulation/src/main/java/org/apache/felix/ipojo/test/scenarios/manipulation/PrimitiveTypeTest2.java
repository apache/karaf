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
package org.apache.felix.ipojo.test.scenarios.manipulation;

import java.util.Properties;

import org.apache.felix.ipojo.ComponentInstance;
import org.apache.felix.ipojo.junit4osgi.OSGiTestCase;
import org.apache.felix.ipojo.test.scenarios.manipulation.service.PrimitiveManipulationTestService;
import org.apache.felix.ipojo.test.scenarios.util.Utils;
import org.osgi.framework.ServiceReference;

/**
 * Check the manipulation of primitive type (boxed and unboxed).
 * The targeted implementation contains numbers.
 */
public class PrimitiveTypeTest2 extends OSGiTestCase {

    ComponentInstance instance; // Instance under test

    PrimitiveManipulationTestService prim;

    ServiceReference prim_ref;

    public void setUp() {
        Properties p1 = new Properties();
        p1.put("name", "primitives");
        instance = Utils.getComponentInstance(context, "Manipulation-PrimitiveManipulationTesterA", p1);
        assertTrue("check instance state", instance.getState() == ComponentInstance.VALID);
        prim_ref = Utils.getServiceReferenceByName(context, PrimitiveManipulationTestService.class.getName(), instance.getInstanceName());
        assertNotNull("Check prim availability", prim_ref);
        prim = (PrimitiveManipulationTestService) context.getService(prim_ref);
    }

    public void tearDown() {
        context.ungetService(prim_ref);
        prim = null;
        instance.dispose();
        instance = null;
    }

    public void testByte() {
        assertEquals("Check - 1", prim.getByte(), 1);
        prim.setByte((byte) 2);
        assertEquals("Check - 2", prim.getByte(), 2);
    }

    public void testShort() {
        assertEquals("Check - 1", prim.getShort(), 1);
        prim.setShort((short) 2);
        assertEquals("Check - 2", prim.getShort(), 2);
    }

    public void testInt() {
        assertEquals("Check - 1", prim.getInt(), 1);
        prim.setInt((int) 2);
        assertEquals("Check - 2", prim.getInt(), 2);
    }

    public void testLong() {
        assertEquals("Check - 1", prim.getLong(), 1);
        prim.setLong((long) 2);
        assertEquals("Check - 2", prim.getLong(), 2);
    }

    public void testLong2() {
        assertEquals("Check - 1", prim.getLong(), 1);
        prim.setLong(2, "ss");
        assertEquals("Check - 2", prim.getLong(), 2);
    }

    //TODO : how to tests these two Java 5 methods ...
//    public void testLongFromObject() {
//        assertEquals("Check - 1", prim.getLong(), 1);
//        Long l = new Long(2);
//        prim.setLong(l);
//        assertEquals("Check - 2", prim.getLong(), 2);
//    }
//
//    public void testLongFromObject2() {
//        assertEquals("Check - 1", prim.getLong(), 1);
//        Long l = new Long(2);
//        prim.setLong(l, "ss");
//        assertEquals("Check - 2", prim.getLong(), 2);
//    }

    public void testFloat() {
        assertEquals("Check - 1", prim.getFloat(), 1.1f);
        prim.setFloat(2.2f);
        assertEquals("Check - 2", prim.getFloat(), 2.2f);
    }

    public void testDouble() {
        assertEquals("Check - 1", prim.getDouble(), 1.1);
        prim.setDouble(2.2);
        assertEquals("Check - 2", prim.getDouble(), 2.2);
    }

    public void testBoolean() {
        assertFalse("Check - 1", prim.getBoolean());
        prim.setBoolean(true);
        assertTrue("Check - 2", prim.getBoolean());
    }

    public void testChar() {
        assertEquals("Check - 1", prim.getChar(), 'a');
        prim.setChar('b');
        assertEquals("Check - 2", prim.getChar(), 'b');
    }

}
