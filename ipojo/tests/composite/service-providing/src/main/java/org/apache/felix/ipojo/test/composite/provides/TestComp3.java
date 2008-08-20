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
package org.apache.felix.ipojo.test.composite.provides;

import java.util.Properties;

import org.apache.felix.ipojo.ComponentFactory;
import org.apache.felix.ipojo.ComponentInstance;
import org.apache.felix.ipojo.Factory;
import org.apache.felix.ipojo.architecture.Architecture;
import org.apache.felix.ipojo.junit4osgi.OSGiTestCase;
import org.apache.felix.ipojo.test.composite.component.TotoProvider;
import org.apache.felix.ipojo.test.composite.service.Tota;
import org.apache.felix.ipojo.test.composite.util.Utils;
import org.osgi.framework.ServiceReference;

public class TestComp3 extends OSGiTestCase {

    private ComponentFactory tataFactory;
    private ComponentFactory totoFactory;
    
    private ComponentInstance totoProv, totoProv2;
    private ComponentInstance under;
	private ComponentFactory tataFactory2;  
    
    public void setUp() {
        tataFactory = (ComponentFactory) Utils.getFactoryByName(context, "tata");
        totoFactory = (ComponentFactory) Utils.getFactoryByName(context, "toto");
        tataFactory2 = (ComponentFactory) Utils.getFactoryByName(context, "comp-6");
        tataFactory2.stop();
        tataFactory.stop();
        
        Properties props = new Properties();
        props.put("instance.name","toto provider");
        try {
            totoProv = totoFactory.createComponentInstance(props);
        } catch(Exception e) {
            e.printStackTrace();
        }
        
        Properties props3 = new Properties();
        props3.put("instance.name","toto provider 2");
        try {
            totoProv2 = totoFactory.createComponentInstance(props3);
        } catch(Exception e) {
            e.printStackTrace();
        }
        
        totoProv.stop();
        totoProv2.stop();
        
        Factory factory = Utils.getFactoryByName(context, "comp-3");
        Properties props2 = new Properties();
        props2.put("instance.name","ff");
        try {
            under = factory.createComponentInstance(props2);
        } catch(Exception e) {
            e.printStackTrace();
        }
         
    }
    
    public void tearDown() {  
        tataFactory.start();
        totoProv.dispose();
        totoProv = null;
        totoProv2.dispose();
        totoProv2 = null;
        tataFactory2.start();
        
        // Reset counters
        TotoProvider.toto = 0;
        TotoProvider.toto_2 = 0;
        TotoProvider.toto_3 = 0;
        TotoProvider.toto_4 = 0;
        TotoProvider.toto1 = 0;
    }
    
    public void testSimple() {
        // Neither factory nor instance
        assertTrue("Assert under state - 1", under.getState() == ComponentInstance.INVALID);
        assertNull("Assert no tota service - 1", context.getServiceReference(Tota.class.getName()));
        
        // Start the importer
        totoProv.start();
        assertTrue("Assert under state - 2 ("+under.getState()+")", under.getState() == ComponentInstance.INVALID);
        assertNull("Assert no tota service - 2", context.getServiceReference(Tota.class.getName()));
        
        // Start the factory
        tataFactory.start();
        assertTrue("Assert under state - 3", under.getState() == ComponentInstance.VALID);
        assertNotNull("Assert tota service - 3", context.getServiceReference(Tota.class.getName()));
        ServiceReference ref = context.getServiceReference(Tota.class.getName());
        Tota tota = (Tota) context.getService(ref);
        invokeAll(tota);
        // Check toto
        Properties props = tota.getProps();
        Integer toto = (Integer) props.get("toto");
        Integer toto_2 = (Integer) props.get("toto_2");
        Integer toto_3 = (Integer) props.get("toto_3");
        Integer toto_4 = (Integer) props.get("toto_4");
        Integer toto_1 = (Integer) props.get("toto1");
        assertEquals("Assert toto - 3 ("+toto.intValue()+")", toto.intValue(), 1);
        assertEquals("Assert toto_2 - 3", toto_2.intValue(), 1);
        assertEquals("Assert toto_3 - 3", toto_3.intValue(), 1);
        assertEquals("Assert toto_4 - 3", toto_4.intValue(), 0);
        assertEquals("Assert toto1 - 3 (" + toto_1.intValue() + ")", toto_1.intValue(), 1);
        //Check tata
        props = tota.getPropsTata();
        Integer tata = (Integer) props.get("tata");
        assertEquals("Assert tata - 3", tata.intValue(), 1);
        
        context.ungetService(ref);
        tota = null;
        
        // Start a second import
        totoProv2.start();
        assertTrue("Assert under state - 4", under.getState() == ComponentInstance.VALID);
        assertNotNull("Assert tota service - 4", context.getServiceReference(Tota.class.getName()));
        ref = context.getServiceReference(Tota.class.getName());
        tota = (Tota) context.getService(ref);
        invokeAll(tota);
        props = tota.getProps();
        toto = (Integer) props.get("toto");
        toto_2 = (Integer) props.get("toto_2");
        toto_3 = (Integer) props.get("toto_3");
        toto_4 = (Integer) props.get("toto_4");
        toto_1 = (Integer) props.get("toto1");
        assertEquals("Assert toto - 4 ("+toto.intValue()+")", toto.intValue(), 2);
        assertEquals("Assert toto_2 - 4 ("+toto_2.intValue()+")", toto_2.intValue(), 2);
        assertEquals("Assert toto_3 - 4", toto_3.intValue(), 2);
        assertEquals("Assert toto_4 - 4", toto_4.intValue(), 0);
        assertEquals("Assert toto1 - 4", toto_1.intValue(), 2);
        //Check tata
        props = tota.getPropsTata();
        tata = (Integer) props.get("tata");
        assertEquals("Assert tata - 4", tata.intValue(), 2);
        context.ungetService(ref);
        tota = null;
        
        // Stop the factory
        tataFactory.stop();
        assertTrue("Assert under state - 5", under.getState() == ComponentInstance.INVALID);
        assertNull("Assert no tota service - 5", context.getServiceReference(Tota.class.getName()));
        
        totoProv2.stop();
        totoProv.stop();
        tataFactory.start();
        assertTrue("Assert under state - 6", under.getState() == ComponentInstance.VALID);
        assertNotNull("Assert tota service - 6", context.getServiceReference(Tota.class.getName()));
        ref = context.getServiceReference(Tota.class.getName());
        tota = (Tota) context.getService(ref);
        invokeAllOpt(tota);
        //Check tata
        props = tota.getPropsTata();
        tata = (Integer) props.get("tata");
        assertEquals("Assert tata - 4", tata.intValue(), 1);
        context.ungetService(ref);
        tota = null;
        
        // Is arch exposed
        assertNotNull("Test arch", Utils.getServiceReferenceByName(context, Architecture.class.getName(), "ff"));
        
        tataFactory.stop();
        
        assertTrue("Assert under state - 7", under.getState() == ComponentInstance.INVALID);
        assertNotNull("Test arch-2", Utils.getServiceReferenceByName(context, Architecture.class.getName(), "ff"));
        assertNull("Assert no tota service - 7", context.getServiceReference(Tota.class.getName()));
        
        under.dispose();
        under = null;
    }

    private void invoke(Tota tota) {
        tota.tata();
        
        assertEquals("Assert invoke tataint", tota.tataInt(2), 2);
        assertEquals("Assert invoke tataLong", tota.tataLong(2), 2);
        assertEquals("Assert invoke tataDouble", tota.tataDouble(2), 2);
        assertEquals("Assert invoke tataChar", tota.tataChar('a'), 'a');
        assertTrue("Assert invoke tataBoolean", tota.tataBoolean(true));
        assertEquals("Assert invoke tataByte", tota.tataByte((byte)2), 2);
        assertEquals("Assert invoke tataShort", tota.tataShort((short)5), 5);
        assertEquals("Assert invoke tataFloat", tota.tataFloat(5), 5);
        
    }
    
    
    private void invokeStr(Tota tota) {
        tota.tataStr();
    }
    
    private void invokeToto(Tota tota) {
        tota.toto();
        assertEquals("Assert toto", tota.toto("foo"), "foo");
        tota.toto(1,2);
        tota.toto1("foo");
    }
    
    private void invokeAll(Tota tota) {
        invoke(tota);
        //invokeArrays(tota);
        invokeStr(tota);
        //invokeTata(tota);
        //invokeTata1(tota);
        //invokeTata5(tota);
        //invokeAdd(tota);
        invokeToto(tota);
    }
    
    private void invokeAllOpt(Tota tota) {
        invoke(tota);
        //invokeArrays(tota);
        invokeStr(tota);
        //invokeTata(tota);
        //invokeTata1(tota);
        //invokeTata5(tota);
        //invokeAdd(tota);
        invokeTotoOpt(tota);
    }
    
    private void invokeTotoOpt(Tota tota) {
        try {
            tota.toto();
            fail("UnsupportedOperationException expected");
        } catch(UnsupportedOperationException e) { }
        
        try {
            assertEquals("Assert toto", tota.toto("foo"), "foo");
            fail("UnsupportedOperationException expected");
        } catch(UnsupportedOperationException e) { }
        
        
        try {
            tota.toto(1,2);
            fail("UnsupportedOperationException expected");
        } catch(UnsupportedOperationException e) { }
        
        try {
            tota.toto1("foo");
            fail("UnsupportedOperationException expected");
        } catch(UnsupportedOperationException e) { }
    }
    

}
