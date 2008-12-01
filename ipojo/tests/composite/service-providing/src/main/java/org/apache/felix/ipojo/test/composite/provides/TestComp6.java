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
import org.apache.felix.ipojo.test.composite.service.Tata;
import org.apache.felix.ipojo.test.composite.service.Toto;
import org.apache.felix.ipojo.test.composite.util.Utils;
import org.osgi.framework.ServiceReference;

public class TestComp6 extends OSGiTestCase {

    private ComponentFactory tataFactory;
    private ComponentFactory totoFactory;
    
    private ComponentInstance totoProv, totoProv2;
    private ComponentInstance under;
    
    public void setUp() {
        tataFactory = (ComponentFactory) Utils.getFactoryByName(getContext(), "tata");
        totoFactory = (ComponentFactory) Utils.getFactoryByName(getContext(), "toto");
        
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
        
        Factory factory = Utils.getFactoryByName(getContext(), "comp-6");
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
        ServiceReference refToto = Utils.getServiceReferenceByName(getContext(), Toto.class.getName(), "ff");
        ServiceReference refTata = Utils.getServiceReferenceByName(getContext(), Tata.class.getName(), "ff");
        assertNull("Assert no toto service - 1", refToto);
        assertNull("Assert no tata service - 1", refTata);

        // Start the importer
        totoProv.start();
        assertTrue("Assert under state - 2 ("+under.getState()+")", under.getState() == ComponentInstance.INVALID);
        refToto = Utils.getServiceReferenceByName(getContext(), Toto.class.getName(), "ff");
        refTata = Utils.getServiceReferenceByName(getContext(), Tata.class.getName(), "ff");
        assertNull("Assert no toto service - 2", refToto);
        assertNull("Assert no tata service - 2", refTata);

        // Start the factory
        tataFactory.start();
        assertTrue("Assert under state - 3", under.getState() == ComponentInstance.VALID);
        refToto = Utils.getServiceReferenceByName(getContext(), Toto.class.getName(), "ff");
        refTata = Utils.getServiceReferenceByName(getContext(), Tata.class.getName(), "ff");
        assertNotNull("Assert toto service - 3", refToto);
        assertNotNull("Assert tata service - 3", refTata);
        Toto toto = (Toto) getContext().getService(refToto);
        Tata tata = (Tata) getContext().getService(refTata);
 
        invokeAll(tata);
        invokeToto(toto);
 
        // Check toto
        Properties props = toto.getProps();
        Integer toto_0 = (Integer) props.get("toto");
        Integer toto_2 = (Integer) props.get("toto_2");
        Integer toto_3 = (Integer) props.get("toto_3");
        Integer toto_4 = (Integer) props.get("toto_4");
        Integer toto_1 = (Integer) props.get("toto1");
        assertEquals("Assert toto - 3 ("+toto_0.intValue()+")", toto_0.intValue(), 1);
        assertEquals("Assert toto_2 - 3", toto_2.intValue(), 1);
        assertEquals("Assert toto_3 - 3", toto_3.intValue(), 1);
        assertEquals("Assert toto_4 - 3", toto_4.intValue(), 0);
        assertEquals("Assert toto1 - 3 (" + toto_1.intValue() + ")", toto_1.intValue(), 1);
        //Check tata
        props = tata.getPropsTata();
        Integer tata_0 = (Integer) props.get("tata");
        assertEquals("Assert tata - 3", tata_0.intValue(), 1);

        getContext().ungetService(refToto);
        getContext().ungetService(refTata);
        toto = null;
        tata = null;
        
        // Start a second import
        totoProv2.start();
        assertTrue("Assert under state - 4", under.getState() == ComponentInstance.VALID);
        refToto = Utils.getServiceReferenceByName(getContext(), Toto.class.getName(), "ff");
        refTata = Utils.getServiceReferenceByName(getContext(), Tata.class.getName(), "ff");
        assertNotNull("Assert toto service - 4", refToto);
        assertNotNull("Assert tata service - 4", refTata);
        
        toto = (Toto) getContext().getService(refToto);
        tata = (Tata) getContext().getService(refTata);
        invokeAll(tata);
        invokeToto(toto);

        // Check toto
        props = toto.getProps();
        toto_0 = (Integer) props.get("toto");
        toto_2 = (Integer) props.get("toto_2");
        toto_3 = (Integer) props.get("toto_3");
        toto_4 = (Integer) props.get("toto_4");
        toto_1 = (Integer) props.get("toto1");
        assertEquals("Assert toto - 4 ("+toto_0.intValue()+")", toto_0.intValue(), 2);
        assertEquals("Assert toto_2 - 4 ("+toto_2.intValue()+")", toto_2.intValue(), 2);
        assertEquals("Assert toto_3 - 4", toto_3.intValue(), 2);
        assertEquals("Assert toto_4 - 4", toto_4.intValue(), 0);
        assertEquals("Assert toto1 - 4", toto_1.intValue(), 3);
        //Check tata
        props = tata.getPropsTata();
        tata_0 = (Integer) props.get("tata");
        assertEquals("Assert tata - 4", tata_0.intValue(), 2);
        getContext().ungetService(refToto);
        getContext().ungetService(refTata);
        toto = null;
        tata = null;

        // Stop the factory
        tataFactory.stop();
        assertTrue("Assert under state - 5", under.getState() == ComponentInstance.INVALID);
        refToto = Utils.getServiceReferenceByName(getContext(), Toto.class.getName(), "ff");
        refTata = Utils.getServiceReferenceByName(getContext(), Tata.class.getName(), "ff");
        assertNull("Assert no toto service - 5", refToto);
        assertNull("Assert no tata service - 5", refTata);
 
        totoProv2.stop();
        totoProv.stop();
        tataFactory.start();
        assertTrue("Assert under state - 6", under.getState() == ComponentInstance.VALID);
        refToto = Utils.getServiceReferenceByName(getContext(), Toto.class.getName(), "ff");
        refTata = Utils.getServiceReferenceByName(getContext(), Tata.class.getName(), "ff");
        assertNotNull("Assert toto service - 6", refToto);
        assertNotNull("Assert tata service - 6", refTata);
        toto = (Toto) getContext().getService(refToto);
        tata = (Tata) getContext().getService(refTata);
 
        invokeAll(tata);
        invokeTotoOpt(toto);
        //  Check tata
        props = tata.getPropsTata();
        tata_0 = (Integer) props.get("tata");
        assertEquals("Assert tata - 6", tata_0.intValue(), 1);
        getContext().ungetService(refToto);
        getContext().ungetService(refTata);
        toto = null;
        tata = null;
 
        // Is arch exposed
        assertNotNull("Test arch", Utils.getServiceReferenceByName(getContext(), Architecture.class.getName(), "ff"));
        
        tataFactory.stop();
        
        assertTrue("Assert under state - 7", under.getState() == ComponentInstance.INVALID);
        assertNotNull("Test arch-2", Utils.getServiceReferenceByName(getContext(), Architecture.class.getName(), "ff"));
        refToto = Utils.getServiceReferenceByName(getContext(), Toto.class.getName(), "ff");
        refTata = Utils.getServiceReferenceByName(getContext(), Tata.class.getName(), "ff");
        assertNull("Assert no toto service - 7", refToto);
        assertNull("Assert no tata service - 7", refTata);
        
        under.dispose();
        under = null;
    }

    private void invoke(Tata tota) {
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
    
    
    private void invokeStr(Tata tota) {
        tota.tataStr();
    }
    
    private void invokeToto(Toto tota) {
        tota.toto();
        assertEquals("Assert toto", tota.toto("foo"), "foo");
        tota.toto(1,2);
        tota.toto1("foo");
    }
    
    private void invokeAll(Tata tota) {
        invoke(tota);
        //invokeArrays(tota);
        invokeStr(tota);
        //invokeTata(tota);
        //invokeTata1(tota);
        //invokeTata5(tota);
        //invokeAdd(tota);
    }
    
    private void invokeTotoOpt(Toto tota) {
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
