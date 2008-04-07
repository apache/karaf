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
package org.apache.felix.ipojo.test.scenarios.configuration;

import java.util.Properties;

import org.apache.felix.ipojo.ComponentInstance;
import org.apache.felix.ipojo.Factory;
import org.apache.felix.ipojo.junit4osgi.OSGiTestCase;
import org.apache.felix.ipojo.test.scenarios.service.CheckService;
import org.apache.felix.ipojo.test.scenarios.util.Utils;
import org.osgi.framework.ServiceReference;

public class TestSuperMethodProperties extends OSGiTestCase {
    
    ComponentInstance instance;

    public void setUp() {
        Factory fact = Utils.getFactoryByName(context, "ParentMethodConfigurableCheckService");
        Properties props = new Properties();
        props.put("name", "under-test");
        props.put("b", "1");
        props.put("s", "1");
        props.put("i", "1");
        props.put("l", "1");
        props.put("d", "1");
        props.put("f", "1");
        props.put("c", "a");
        props.put("bool", "true");
        props.put("bs", "{1,2,3}");
        props.put("ss", "{1,2,3}");
        props.put("is", "{1,2,3}");
        props.put("ls", "{1,2,3}");
        props.put("ds", "{1,2,3}");
        props.put("fs", "{1,2,3}");
        props.put("cs", "{a,b,c}");
        props.put("bools", "{true,true,true}");
        props.put("string", "foo");
        props.put("strings", "{foo, bar, baz}");
        
        try {
            instance = fact.createComponentInstance(props);
        } catch (Exception e) {
           fail("Cannot create the under-test instance : " + e.getMessage());
        }
        
        
    }
    
    public void tearDown() {
        instance.dispose();
        instance = null;
    }
    
    public void testConfigurationPrimitive() {
        ServiceReference ref = Utils.getServiceReferenceByName(context, CheckService.class.getName(), instance.getInstanceName());
        assertNotNull("Test check service availability", ref);
        CheckService check = (CheckService) context.getService(ref);
        Properties props = check.getProps();
        
        Byte b = (Byte) props.get("b");
        Short s = (Short) props.get("s");
        Integer i = (Integer) props.get("i");
        Long l = (Long) props.get("l");
        Double d = (Double) props.get("d");
        Float f = (Float) props.get("f");
        Character c = (Character) props.get("c");
        Boolean bool = (Boolean) props.get("bool");
                
        assertEquals("Check b", b, new Byte("1"));
        assertEquals("Check s", s, new Short("1"));
        assertEquals("Check i", i, new Integer("1"));
        assertEquals("Check l", l, new Long("1"));
        assertEquals("Check d", d, new Double("1"));
        assertEquals("Check f", f, new Float("1"));
        assertEquals("Check c", c, new Character('a'));
        assertEquals("Check bool", bool, new Boolean("true"));
        
//        Integer upb = (Integer) props.get("upb");
//        Integer ups = (Integer) props.get("ups");
//        Integer upi = (Integer) props.get("upi");
//        Integer upl = (Integer) props.get("upl");
//        Integer upd = (Integer) props.get("upd");
//        Integer upf = (Integer) props.get("upf");
//        Integer upc = (Integer) props.get("upc");
//        Integer upbool = (Integer) props.get("upbool");
//        
//        assertEquals("Check upb", upb, new Integer(1));
//        assertEquals("Check ups", ups, new Integer(1));
//        assertEquals("Check upi", upi, new Integer(1));
//        assertEquals("Check upl", upl, new Integer(1));
//        assertEquals("Check upd", upd, new Integer(1));
//        assertEquals("Check upf", upf, new Integer(1));
//        assertEquals("Check upc", upc, new Integer(1));
//        assertEquals("Check upbool", upbool, new Integer(1));
        
        reconfigure();
        
        ref = Utils.getServiceReferenceByName(context, CheckService.class.getName(), instance.getInstanceName());
        assertNotNull("Test check service availability", ref);
        check = (CheckService) context.getService(ref);
        props = check.getProps();
        
        b = (Byte) props.get("b");
        s = (Short) props.get("s");
        i = (Integer) props.get("i");
        l = (Long) props.get("l");
        d = (Double) props.get("d");
        f = (Float) props.get("f");
        c = (Character) props.get("c");
        bool = (Boolean) props.get("bool");
        
        assertEquals("2) Check b ("+b+")", b, new Byte("2"));
        assertEquals("2) Check s", s, new Short("2"));
        assertEquals("2) Check i", i, new Integer("2"));
        assertEquals("2) Check l", l, new Long("2"));
        assertEquals("2) Check d", d, new Double("2"));
        assertEquals("2) Check f", f, new Float("2"));
        assertEquals("2) Check c", c, new Character('b'));
        assertEquals("2) Check bool", bool, new Boolean("false"));
        
//        upb = (Integer) props.get("upb");
//        ups = (Integer) props.get("ups");
//        upi = (Integer) props.get("upi");
//        upl = (Integer) props.get("upl");
//        upd = (Integer) props.get("upd");
//        upf = (Integer) props.get("upf");
//        upc = (Integer) props.get("upc");
//        upbool = (Integer) props.get("upbool");
//        
//        assertEquals("2) Check upb", upb, new Integer(2));
//        assertEquals("2) Check ups", ups, new Integer(2));
//        assertEquals("2) Check upi", upi, new Integer(2));
//        assertEquals("2) Check upl", upl, new Integer(2));
//        assertEquals("2) Check upd", upd, new Integer(2));
//        assertEquals("2) Check upf", upf, new Integer(2));
//        assertEquals("2) Check upc", upc, new Integer(2));
//        assertEquals("2) Check upbool", upbool, new Integer(2));
        
    }

    public void testConfigurationPrimitiveString() {
        ServiceReference ref = Utils.getServiceReferenceByName(context, CheckService.class.getName(), instance.getInstanceName());
        assertNotNull("Test check service availability", ref);
        CheckService check = (CheckService) context.getService(ref);
        Properties props = check.getProps();
        
        Byte b = (Byte) props.get("b");
        Short s = (Short) props.get("s");
        Integer i = (Integer) props.get("i");
        Long l = (Long) props.get("l");
        Double d = (Double) props.get("d");
        Float f = (Float) props.get("f");
        Character c = (Character) props.get("c");
        Boolean bool = (Boolean) props.get("bool");
                
        assertEquals("Check b", b, new Byte("1"));
        assertEquals("Check s", s, new Short("1"));
        assertEquals("Check i", i, new Integer("1"));
        assertEquals("Check l", l, new Long("1"));
        assertEquals("Check d", d, new Double("1"));
        assertEquals("Check f", f, new Float("1"));
        assertEquals("Check c", c, new Character('a'));
        assertEquals("Check bool", bool, new Boolean("true"));
        
//        Integer upb = (Integer) props.get("upb");
//        Integer ups = (Integer) props.get("ups");
//        Integer upi = (Integer) props.get("upi");
//        Integer upl = (Integer) props.get("upl");
//        Integer upd = (Integer) props.get("upd");
//        Integer upf = (Integer) props.get("upf");
//        Integer upc = (Integer) props.get("upc");
//        Integer upbool = (Integer) props.get("upbool");
//        
//        assertEquals("Check upb", upb, new Integer(1));
//        assertEquals("Check ups", ups, new Integer(1));
//        assertEquals("Check upi", upi, new Integer(1));
//        assertEquals("Check upl", upl, new Integer(1));
//        assertEquals("Check upd", upd, new Integer(1));
//        assertEquals("Check upf", upf, new Integer(1));
//        assertEquals("Check upc", upc, new Integer(1));
//        assertEquals("Check upbool", upbool, new Integer(1));
        
        reconfigureString();
        
        ref = Utils.getServiceReferenceByName(context, CheckService.class.getName(), instance.getInstanceName());
        assertNotNull("Test check service availability", ref);
        check = (CheckService) context.getService(ref);
        props = check.getProps();
        
        b = (Byte) props.get("b");
        s = (Short) props.get("s");
        i = (Integer) props.get("i");
        l = (Long) props.get("l");
        d = (Double) props.get("d");
        f = (Float) props.get("f");
        c = (Character) props.get("c");
        bool = (Boolean) props.get("bool");
        
        assertEquals("2) Check b ("+b+")", b, new Byte("2"));
        assertEquals("2) Check s", s, new Short("2"));
        assertEquals("2) Check i", i, new Integer("2"));
        assertEquals("2) Check l", l, new Long("2"));
        assertEquals("2) Check d", d, new Double("2"));
        assertEquals("2) Check f", f, new Float("2"));
        assertEquals("2) Check c", c, new Character('b'));
        assertEquals("2) Check bool", bool, new Boolean("false"));
        
//        upb = (Integer) props.get("upb");
//        ups = (Integer) props.get("ups");
//        upi = (Integer) props.get("upi");
//        upl = (Integer) props.get("upl");
//        upd = (Integer) props.get("upd");
//        upf = (Integer) props.get("upf");
//        upc = (Integer) props.get("upc");
//        upbool = (Integer) props.get("upbool");
//        
//        assertEquals("2) Check upb", upb, new Integer(2));
//        assertEquals("2) Check ups", ups, new Integer(2));
//        assertEquals("2) Check upi", upi, new Integer(2));
//        assertEquals("2) Check upl", upl, new Integer(2));
//        assertEquals("2) Check upd", upd, new Integer(2));
//        assertEquals("2) Check upf", upf, new Integer(2));
//        assertEquals("2) Check upc", upc, new Integer(2));
//        assertEquals("2) Check upbool", upbool, new Integer(2));
        
    }
    
    public void testConfigurationPrimitiveArrays() {
        ServiceReference ref = Utils.getServiceReferenceByName(context, CheckService.class.getName(), instance.getInstanceName());
        assertNotNull("Test check service availability", ref);
        CheckService check = (CheckService) context.getService(ref);
        Properties props = check.getProps();
        
        byte[] b = (byte[]) props.get("bs");
        short[] s = (short[]) props.get("ss");
        int[] i = (int[]) props.get("is");
        long[] l = (long[]) props.get("ls");
        double[] d = (double[]) props.get("ds");
        float[] f = (float[]) props.get("fs");
        char[] c = (char[]) props.get("cs");
        boolean[] bool = (boolean[]) props.get("bools");
                
        assertEquals("Check b 0", b[0], 1);
        assertEquals("Check b 1", b[1], 2);
        assertEquals("Check b 2", b[2], 3);
        assertEquals("Check s 0", s[0], 1);
        assertEquals("Check s 1", s[1], 2);
        assertEquals("Check s 2", s[2], 3);
        assertEquals("Check i 0", i[0], 1);
        assertEquals("Check i 1", i[1], 2);
        assertEquals("Check i 2", i[2], 3);
        assertEquals("Check l 0", l[0], 1);
        assertEquals("Check l 1", l[1], 2);
        assertEquals("Check l 2", l[2], 3);
        assertEquals("Check d 0", d[0], 1);
        assertEquals("Check d 1", d[1], 2);
        assertEquals("Check d 2", d[2], 3);
        assertEquals("Check f 0", f[0], 1);
        assertEquals("Check f 1", f[1], 2);
        assertEquals("Check f 2", f[2], 3);
        assertEquals("Check c 0", c[0], 'a');
        assertEquals("Check c 1", c[1], 'b');
        assertEquals("Check c 2", c[2], 'c');
        assertTrue("Check bool 0", bool[0]);
        assertTrue("Check bool 1", bool[0]);
        assertTrue("Check bool 2", bool[0]);
        
//        Integer upb = (Integer) props.get("upbs");
//        Integer ups = (Integer) props.get("upss");
//        Integer upi = (Integer) props.get("upis");
//        Integer upl = (Integer) props.get("upls");
//        Integer upd = (Integer) props.get("upds");
//        Integer upf = (Integer) props.get("upfs");
//        Integer upc = (Integer) props.get("upcs");
//        Integer upbool = (Integer) props.get("upbools");
//        
//        assertEquals("Check upb", upb, new Integer(1));
//        assertEquals("Check ups", ups, new Integer(1));
//        assertEquals("Check upi", upi, new Integer(1));
//        assertEquals("Check upl", upl, new Integer(1));
//        assertEquals("Check upd", upd, new Integer(1));
//        assertEquals("Check upf", upf, new Integer(1));
//        assertEquals("Check upc", upc, new Integer(1));
//        assertEquals("Check upbool", upbool, new Integer(1));
        
        reconfigure();
        
        ref = Utils.getServiceReferenceByName(context, CheckService.class.getName(), instance.getInstanceName());
        assertNotNull("Test check service availability", ref);
        check = (CheckService) context.getService(ref);
        props = check.getProps();
        
        b = (byte[]) props.get("bs");
        s = (short[]) props.get("ss");
        i = (int[]) props.get("is");
        l = (long[]) props.get("ls");
        d = (double[]) props.get("ds");
        f = (float[]) props.get("fs");
        c = (char[]) props.get("cs");
        bool = (boolean[]) props.get("bools");
        
        assertEquals("2) Check b 0", b[0], 3);
        assertEquals("2) Check b 1", b[1], 2);
        assertEquals("2) Check b 2", b[2], 1);
        assertEquals("2) Check s 0", s[0], 3);
        assertEquals("2) Check s 1", s[1], 2);
        assertEquals("2) Check s 2", s[2], 1);
        assertEquals("2) Check i 0", i[0], 3);
        assertEquals("2) Check i 1", i[1], 2);
        assertEquals("2) Check i 2", i[2], 1);
        assertEquals("2) Check l 0", l[0], 3);
        assertEquals("2) Check l 1", l[1], 2);
        assertEquals("2) Check l 2", l[2], 1);
        assertEquals("2) Check d 0", d[0], 3);
        assertEquals("2) Check d 1", d[1], 2);
        assertEquals("2) Check d 2", d[2], 1);
        assertEquals("2) Check f 0", f[0], 3);
        assertEquals("2) Check f 1", f[1], 2);
        assertEquals("2) Check f 2", f[2], 1);
        assertEquals("2) Check c 0", c[0], 'c');
        assertEquals("2) Check c 1", c[1], 'b');
        assertEquals("2) Check c 2", c[2], 'a');
        assertFalse("2) Check bool 0", bool[0]);
        assertFalse("2) Check bool 1", bool[0]);
        assertFalse("2) Check bool 2", bool[0]);
        
//        upb = (Integer) props.get("upbs");
//        ups = (Integer) props.get("upss");
//        upi = (Integer) props.get("upis");
//        upl = (Integer) props.get("upls");
//        upd = (Integer) props.get("upds");
//        upf = (Integer) props.get("upfs");
//        upc = (Integer) props.get("upcs");
//        upbool = (Integer) props.get("upbools");
//        
//        assertEquals("2) Check upb", upb, new Integer(2));
//        assertEquals("2) Check ups", ups, new Integer(2));
//        assertEquals("2) Check upi", upi, new Integer(2));
//        assertEquals("2) Check upl", upl, new Integer(2));
//        assertEquals("2) Check upd", upd, new Integer(2));
//        assertEquals("2) Check upf", upf, new Integer(2));
//        assertEquals("2) Check upc", upc, new Integer(2));
//        assertEquals("2) Check upbool", upbool, new Integer(2));
        
    }

    public void testConfigurationPrimitiveArraysString() {
        ServiceReference ref = Utils.getServiceReferenceByName(context, CheckService.class.getName(), instance.getInstanceName());
        assertNotNull("Test check service availability", ref);
        CheckService check = (CheckService) context.getService(ref);
        Properties props = check.getProps();
        
        byte[] b = (byte[]) props.get("bs");
        short[] s = (short[]) props.get("ss");
        int[] i = (int[]) props.get("is");
        long[] l = (long[]) props.get("ls");
        double[] d = (double[]) props.get("ds");
        float[] f = (float[]) props.get("fs");
        char[] c = (char[]) props.get("cs");
        boolean[] bool = (boolean[]) props.get("bools");
                
        assertEquals("Check b 0", b[0], 1);
        assertEquals("Check b 1", b[1], 2);
        assertEquals("Check b 2", b[2], 3);
        assertEquals("Check s 0", s[0], 1);
        assertEquals("Check s 1", s[1], 2);
        assertEquals("Check s 2", s[2], 3);
        assertEquals("Check i 0", i[0], 1);
        assertEquals("Check i 1", i[1], 2);
        assertEquals("Check i 2", i[2], 3);
        assertEquals("Check l 0", l[0], 1);
        assertEquals("Check l 1", l[1], 2);
        assertEquals("Check l 2", l[2], 3);
        assertEquals("Check d 0", d[0], 1);
        assertEquals("Check d 1", d[1], 2);
        assertEquals("Check d 2", d[2], 3);
        assertEquals("Check f 0", f[0], 1);
        assertEquals("Check f 1", f[1], 2);
        assertEquals("Check f 2", f[2], 3);
        assertEquals("Check c 0", c[0], 'a');
        assertEquals("Check c 1", c[1], 'b');
        assertEquals("Check c 2", c[2], 'c');
        assertTrue("Check bool 0", bool[0]);
        assertTrue("Check bool 1", bool[0]);
        assertTrue("Check bool 2", bool[0]);
        
//        Integer upb = (Integer) props.get("upbs");
//        Integer ups = (Integer) props.get("upss");
//        Integer upi = (Integer) props.get("upis");
//        Integer upl = (Integer) props.get("upls");
//        Integer upd = (Integer) props.get("upds");
//        Integer upf = (Integer) props.get("upfs");
//        Integer upc = (Integer) props.get("upcs");
//        Integer upbool = (Integer) props.get("upbools");
//        
//        assertEquals("Check upb", upb, new Integer(1));
//        assertEquals("Check ups", ups, new Integer(1));
//        assertEquals("Check upi", upi, new Integer(1));
//        assertEquals("Check upl", upl, new Integer(1));
//        assertEquals("Check upd", upd, new Integer(1));
//        assertEquals("Check upf", upf, new Integer(1));
//        assertEquals("Check upc", upc, new Integer(1));
//        assertEquals("Check upbool", upbool, new Integer(1));
        
        reconfigureString();
        
        ref = Utils.getServiceReferenceByName(context, CheckService.class.getName(), instance.getInstanceName());
        assertNotNull("Test check service availability", ref);
        check = (CheckService) context.getService(ref);
        props = check.getProps();
        
        b = (byte[]) props.get("bs");
        s = (short[]) props.get("ss");
        i = (int[]) props.get("is");
        l = (long[]) props.get("ls");
        d = (double[]) props.get("ds");
        f = (float[]) props.get("fs");
        c = (char[]) props.get("cs");
        bool = (boolean[]) props.get("bools");
        
        assertEquals("2) Check b 0", b[0], 3);
        assertEquals("2) Check b 1", b[1], 2);
        assertEquals("2) Check b 2", b[2], 1);
        assertEquals("2) Check s 0", s[0], 3);
        assertEquals("2) Check s 1", s[1], 2);
        assertEquals("2) Check s 2", s[2], 1);
        assertEquals("2) Check i 0", i[0], 3);
        assertEquals("2) Check i 1", i[1], 2);
        assertEquals("2) Check i 2", i[2], 1);
        assertEquals("2) Check l 0", l[0], 3);
        assertEquals("2) Check l 1", l[1], 2);
        assertEquals("2) Check l 2", l[2], 1);
        assertEquals("2) Check d 0", d[0], 3);
        assertEquals("2) Check d 1", d[1], 2);
        assertEquals("2) Check d 2", d[2], 1);
        assertEquals("2) Check f 0", f[0], 3);
        assertEquals("2) Check f 1", f[1], 2);
        assertEquals("2) Check f 2", f[2], 1);
        assertEquals("2) Check c 0", c[0], 'c');
        assertEquals("2) Check c 1", c[1], 'b');
        assertEquals("2) Check c 2", c[2], 'a');
        assertFalse("2) Check bool 0", bool[0]);
        assertFalse("2) Check bool 1", bool[0]);
        assertFalse("2) Check bool 2", bool[0]);
        
//        upb = (Integer) props.get("upbs");
//        ups = (Integer) props.get("upss");
//        upi = (Integer) props.get("upis");
//        upl = (Integer) props.get("upls");
//        upd = (Integer) props.get("upds");
//        upf = (Integer) props.get("upfs");
//        upc = (Integer) props.get("upcs");
//        upbool = (Integer) props.get("upbools");
//        
//        assertEquals("2) Check upb", upb, new Integer(2));
//        assertEquals("2) Check ups", ups, new Integer(2));
//        assertEquals("2) Check upi", upi, new Integer(2));
//        assertEquals("2) Check upl", upl, new Integer(2));
//        assertEquals("2) Check upd", upd, new Integer(2));
//        assertEquals("2) Check upf", upf, new Integer(2));
//        assertEquals("2) Check upc", upc, new Integer(2));
//        assertEquals("2) Check upbool", upbool, new Integer(2));
        
    }
    
    public void testConfigurationObj() {
        ServiceReference ref = Utils.getServiceReferenceByName(context, CheckService.class.getName(), instance.getInstanceName());
        assertNotNull("Test check service availability", ref);
        CheckService check = (CheckService) context.getService(ref);
        Properties props = check.getProps();
        
        String s = (String) props.get("string");
        String[] ss = (String[]) props.get("strings");
                
        assertEquals("Check string", s, "foo");
        assertEquals("Check strings 0", ss[0], "foo");
        assertEquals("Check strings 1", ss[1], "bar");
        assertEquals("Check strings 2", ss[2], "baz");
        
//        Integer upString = (Integer) props.get("upstring");
//        Integer upStrings = (Integer) props.get("upstrings");
//        
//        assertEquals("Check upString", upString, new Integer(1));
//        assertEquals("Check upStrings", upStrings, new Integer(1));
        
        reconfigure();
        
        ref = Utils.getServiceReferenceByName(context, CheckService.class.getName(), instance.getInstanceName());
        assertNotNull("Test check service availability", ref);
        check = (CheckService) context.getService(ref);
        props = check.getProps();
        
        s = (String) props.get("string");
        ss = (String[]) props.get("strings");
                
        assertEquals("2) Check string", s, "bar");
        assertEquals("2) Check strings 0", ss[0], "baz");
        assertEquals("2) Check strings 1", ss[1], "bar");
        assertEquals("2) Check strings 2", ss[2], "foo");
        
//        upString = (Integer) props.get("upstring");
//        upStrings = (Integer) props.get("upstrings");
//        
//        assertEquals("2) Check upString", upString, new Integer(2));
//        assertEquals("2) Check upStrings", upStrings, new Integer(2));
    }

    public void testConfigurationObjString() {
        ServiceReference ref = Utils.getServiceReferenceByName(context, CheckService.class.getName(), instance.getInstanceName());
        assertNotNull("Test check service availability", ref);
        CheckService check = (CheckService) context.getService(ref);
        Properties props = check.getProps();
        
        String s = (String) props.get("string");
        String[] ss = (String[]) props.get("strings");
                
        assertEquals("Check string", s, "foo");
        assertEquals("Check strings 0", ss[0], "foo");
        assertEquals("Check strings 1", ss[1], "bar");
        assertEquals("Check strings 2", ss[2], "baz");
        
//        Integer upString = (Integer) props.get("upstring");
//        Integer upStrings = (Integer) props.get("upstrings");
//        
//        assertEquals("Check upString", upString, new Integer(1));
//        assertEquals("Check upStrings", upStrings, new Integer(1));
        
        reconfigureString();
        
        ref = Utils.getServiceReferenceByName(context, CheckService.class.getName(), instance.getInstanceName());
        assertNotNull("Test check service availability", ref);
        check = (CheckService) context.getService(ref);
        props = check.getProps();
        
        s = (String) props.get("string");
        ss = (String[]) props.get("strings");
                
        assertEquals("2) Check string", s, "bar");
        assertEquals("2) Check strings 0", ss[0], "baz");
        assertEquals("2) Check strings 1", ss[1], "bar");
        assertEquals("2) Check strings 2", ss[2], "foo");
        
//        upString = (Integer) props.get("upstring");
//        upStrings = (Integer) props.get("upstrings");
//        
//        assertEquals("2) Check upString", upString, new Integer(2));
//        assertEquals("2) Check upStrings", upStrings, new Integer(2));
    }
    
    private void reconfigure() {
        Properties props2 = new Properties();
        props2.put("name", "under-test");
        props2.put("b", new Byte("2"));
        props2.put("s", new Short("2"));
        props2.put("i", new Integer("2"));
        props2.put("l", new Long("2"));
        props2.put("d", new Double("2"));
        props2.put("f", new Float("2"));
        props2.put("c", new Character('b'));
        props2.put("bool", new Boolean(false));
        props2.put("bs", new byte[]{(byte)3,(byte)2,(byte)1});
        props2.put("ss", new short[]{(short)3,(short)2,(short)1});
        props2.put("is", new int[]{3,2,1});
        props2.put("ls", new long[]{3,2,1});
        props2.put("ds", new double[]{3,2,1});
        props2.put("fs", new float[]{3,2,1});
        props2.put("cs", new char[]{'c','b','a'});
        props2.put("bools", new boolean[]{false,false,false});
        props2.put("string", "bar");
        props2.put("strings", new String[]{"baz", "bar", "foo"});
        
        instance.reconfigure(props2);
    }
    
    private void reconfigureString() {
        Properties props2 = new Properties();
        props2.put("name", "under-test");
        props2.put("b", "2");
        props2.put("s", "2");
        props2.put("i", "2");
        props2.put("l", "2");
        props2.put("d", "2");
        props2.put("f", "2");
        props2.put("c", "b");
        props2.put("bool", "false");
        props2.put("bs", "{3, 2,1}");
        props2.put("ss", "{3, 2,1}");
        props2.put("is", "{3, 2,1}");
        props2.put("ls", "{3, 2,1}");
        props2.put("ds", "{3, 2,1}");
        props2.put("fs", "{3, 2,1}");
        props2.put("cs", "{c, b , a}");
        props2.put("bools", "{false,false,false}");
        props2.put("string", "bar");
        props2.put("strings", "{baz, bar, foo}");
        
        instance.reconfigure(props2);
    }

}
