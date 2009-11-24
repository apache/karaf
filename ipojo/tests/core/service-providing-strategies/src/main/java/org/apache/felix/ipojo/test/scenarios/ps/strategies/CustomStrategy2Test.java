package org.apache.felix.ipojo.test.scenarios.ps.strategies;

import org.apache.felix.ipojo.ComponentInstance;
import org.apache.felix.ipojo.PrimitiveInstanceDescription;
import org.apache.felix.ipojo.junit4osgi.OSGiTestCase;
import org.apache.felix.ipojo.junit4osgi.helpers.IPOJOHelper;
import org.apache.felix.ipojo.test.scenarios.component.strategies.FooBarProviderType1;
import org.apache.felix.ipojo.test.scenarios.component.strategies.FooProviderType1;
import org.apache.felix.ipojo.test.scenarios.ps.service.BarService;
import org.apache.felix.ipojo.test.scenarios.ps.service.CheckService;
import org.apache.felix.ipojo.test.scenarios.ps.service.FooService;
import org.osgi.framework.ServiceReference;

import java.util.Properties;

public class CustomStrategy2Test extends OSGiTestCase {


    private IPOJOHelper helper;
    private ComponentInstance cons1, cons2, prov;
    private ComponentInstance cons3, prov2;

    public void setUp() {
        helper = new IPOJOHelper(this);
        cons1 = helper.createComponentInstance("PSS-Cons");
        assertEquals("cons1 invalid", ComponentInstance.INVALID, cons1.getState());
        cons2 = helper.createComponentInstance("PSS-Cons");
        assertEquals("cons2 invalid", ComponentInstance.INVALID, cons2.getState());
        prov = helper.createComponentInstance("PSS-FooProviderType-Custom2");
        prov2 = helper.createComponentInstance("PSS-FooBarProviderType-Custom2");
        cons3 =  helper.createComponentInstance("PSS-ConsBar");
        prov2.stop();
        prov.stop();
    }

    public void tearDown() {
        reset();
    }


    private void reset() {
        FooProviderType1.resetIds();
        FooBarProviderType1.resetIds();
    }

    private void checkCreatedObjects(ComponentInstance ci, int expected) {
        assertEquals("Number of created objects", expected, ((PrimitiveInstanceDescription) ci.getInstanceDescription()).getCreatedObjects().length);
    }

    public void testOneService() {
        prov.start();
        cons2.stop();
        cons1.stop();
        assertEquals("Prov valid", ComponentInstance.VALID, prov.getState());
        ServiceReference ref = helper.getServiceReferenceByName(FooService.class.getName(), prov.getInstanceName());
        assertNotNull("Service available", ref);
        checkCreatedObjects(prov, 0);

        // Step 1 : create start one consumer
        cons1.start();
        ServiceReference refcons1 = helper.getServiceReferenceByName(CheckService.class.getName(), cons1.getInstanceName());
        assertNotNull("Cons1 Service available", refcons1);

        CheckService cs_cons1 = (CheckService) getServiceObject(refcons1);
        Properties props = cs_cons1.getProps();
        Long id = (Long) props.get("id");
        FooService fscons1 = (FooService) props.get("object");
        assertEquals("id 1", 1, id.intValue());
        checkCreatedObjects(prov, 1);


        // Step 2 : create a second consumer
        cons2.start();
        ServiceReference refcons2 = helper.getServiceReferenceByName(CheckService.class.getName(), cons2.getInstanceName());
        assertNotNull("Cons2 Service available", refcons2);

        CheckService cs_cons2 = (CheckService) getServiceObject(refcons2);
        Properties props2 = cs_cons2.getProps();
        Long id2 = (Long) props2.get("id");
        FooService fscons2 = (FooService) props2.get("object");
        assertEquals("id 2", 2, id2.intValue());
        checkCreatedObjects(prov, 2);


        assertNotSame("Two objects", fscons1, fscons2);

        // Step 3 : stop the second provider
        System.out.println("cons2 stopping");
        cons2.stop();
        System.out.println("cons2 stopped");
        checkCreatedObjects(prov, 1);

        // Step 4 : stop the first consumer
        cons1.stop();
        checkCreatedObjects(prov, 0);
    }

    public void testObjectCreation() {
        prov.start();

        // The two consumers are started and use their own objects.
        ServiceReference refcons1 = helper.getServiceReferenceByName(CheckService.class.getName(), cons1.getInstanceName());
        assertNotNull("Cons1 Service available", refcons1);
        CheckService cs_cons1 = (CheckService) getServiceObject(refcons1);
        Properties props = cs_cons1.getProps();
        FooService fscons1 = (FooService) props.get("object");

        ServiceReference refcons2 = helper.getServiceReferenceByName(CheckService.class.getName(), cons2.getInstanceName());
        assertNotNull("Cons2 Service available", refcons2);
        CheckService cs_cons2 = (CheckService) getServiceObject(refcons2);
        Properties props2 = cs_cons2.getProps();
        FooService fscons2 = (FooService) props2.get("object");

        checkCreatedObjects(prov, 2);
        assertNotSame("Two objects", fscons1, fscons2);

        // Stop the provider
        prov.stop();
        // Cons1 and 2 are invalid.
        assertEquals("Cons1 invalidity", ComponentInstance.INVALID, cons1.getState());
        assertEquals("Cons2 invalidity", ComponentInstance.INVALID, cons2.getState());

        // No object created in prov
        checkCreatedObjects(prov, 0);

        // Restart the provider
        prov.start();

        // Consumers are valid.
        assertEquals("Cons1 validity", ComponentInstance.VALID, cons1.getState());
        assertEquals("Cons2 validity", ComponentInstance.VALID, cons2.getState());

        // Check objects
        refcons1 = helper.getServiceReferenceByName(CheckService.class.getName(), cons1.getInstanceName());
        assertNotNull("Cons1 Service available", refcons1);
        cs_cons1 = (CheckService) getServiceObject(refcons1);
        props = cs_cons1.getProps();
        Object fscons3 = (FooService) props.get("object");

        refcons2 = helper.getServiceReferenceByName(CheckService.class.getName(), cons2.getInstanceName());
        assertNotNull("Cons2 Service available", refcons2);
        cs_cons2 = (CheckService) getServiceObject(refcons2);
        props2 = cs_cons2.getProps();
        Object fscons4 = (FooService) props2.get("object");

        checkCreatedObjects(prov, 2);
        assertNotSame("Two objects", fscons3, fscons4);
        assertNotSame("Two new objects - 1", fscons3, fscons1);
        assertNotSame("Two new objects - 2", fscons4, fscons2);

    }

    public void testTwoServices() {
        cons3.stop();
        prov2.start();
        cons1.stop();
        assertEquals("Prov valid", ComponentInstance.VALID, prov2.getState());
        ServiceReference ref = helper.getServiceReferenceByName(FooService.class.getName(), prov2.getInstanceName());
        assertNotNull("Service available", ref);
        ServiceReference refBar = helper.getServiceReferenceByName(BarService.class.getName(), prov2.getInstanceName());
        assertNotNull("Service available", refBar);
        checkCreatedObjects(prov2, 0);

        // Step 1 : create start one consumer
        cons1.start();
        ServiceReference refcons1 = helper.getServiceReferenceByName(CheckService.class.getName(), cons1.getInstanceName());
        assertNotNull("Cons1 Service available", refcons1);

        CheckService cs_cons1 = (CheckService) getServiceObject(refcons1);
        Properties props = cs_cons1.getProps();
        Long id = (Long) props.get("id");
        FooService fscons1 = (FooService) props.get("object");
        assertEquals("id 1", 1, id.intValue());
        checkCreatedObjects(prov2, 1);


        // Step 2 : create a second consumer
        cons3.start();
        ServiceReference refcons2 = helper.getServiceReferenceByName(CheckService.class.getName(), cons3.getInstanceName());
        assertNotNull("Cons2 Service available", refcons2);

        CheckService cs_cons2 = (CheckService) getServiceObject(refcons2);
        Properties props2 = cs_cons2.getProps();
        Long id2 = (Long) props2.get("id");
        FooService fscons2 = (FooService) props2.get("object");
        assertEquals("id 2", 2, id2.intValue());
        checkCreatedObjects(prov2, 2);


        assertNotSame("Two objects", fscons1, fscons2);

        // Step 3 : stop the second provider
        cons3.stop();
        checkCreatedObjects(prov2, 1);

        // Step 4 : stop the first consumer
        cons1.stop();
        checkCreatedObjects(prov, 0);
    }

}