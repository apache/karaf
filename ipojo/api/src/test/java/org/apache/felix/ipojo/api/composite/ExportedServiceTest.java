package org.apache.felix.ipojo.api.composite;


import junit.framework.TestCase;

import org.apache.felix.ipojo.metadata.Element;

public class ExportedServiceTest extends TestCase {
    


//
//<comp:composite name="composite.export.5" architecture="true">
//    <subservice action="import"
//        specification="org.apache.felix.ipojo.test.composite.service.BazService"
//        aggregate="true" optional="true" filter="(!(instance.name=export))"
//        scope="composite" />
//    <comp:provides action="export"
//        specification="org.apache.felix.ipojo.test.composite.service.BazService"
//        filter="(instance.name=foo1)" />
//</comp:composite>

//    <comp:provides action="export"
//      specification="org.apache.felix.ipojo.test.composite.service.BazService" />
    public void testSimple() {
        ExportedService svc = new ExportedService()
        .setSpecification("org.apache.felix.ipojo.test.composite.service.BazService");
        
        Element elem = svc.getElement();
        String name = elem.getName();
        String action = elem.getAttribute("action");
        String spec = elem.getAttribute("specification");
        assertEquals("spec", "org.apache.felix.ipojo.test.composite.service.BazService", spec);
    }
    
    public void testBad() {
        ExportedService svc = new ExportedService()
            //.setSpecification("org.apache.felix.ipojo.test.composite.service.BarService") NO SPEC
            ;
        try {
            svc.getElement();
            fail("Invalid element accepted");
        } catch (IllegalStateException e) {
            // OK
        }
    }
    
 
//  <comp:provides action="export"
//  specification="org.apache.felix.ipojo.test.composite.service.BazService"
//  aggregate="true" />
    public void testAggregate() {
        ExportedService svc = new ExportedService()
        .setSpecification("org.apache.felix.ipojo.test.composite.service.BazService")
        .setAggregate(true);
        
        
        Element elem = svc.getElement();
        String spec = elem.getAttribute("specification");
        String agg = elem.getAttribute("aggregate");

        assertEquals("spec", "org.apache.felix.ipojo.test.composite.service.BazService", spec);
        assertEquals("aggregate", "true", agg);
    }
    
//  <comp:provides action="export"
//  specification="org.apache.felix.ipojo.test.composite.service.BazService"
//  optional="true" />
    public void testOptional() {
        ExportedService svc = new ExportedService()
        .setSpecification("org.apache.felix.ipojo.test.composite.service.BazService")
        .setOptional(true);
        
        
        Element elem = svc.getElement();
        String spec = elem.getAttribute("specification");
        String opt = elem.getAttribute("optional");

        assertEquals("spec", "org.apache.felix.ipojo.test.composite.service.BazService", spec);
        assertEquals("optional", "true", opt);
    }
//  <comp:provides action="export"
//  specification="org.apache.felix.ipojo.test.composite.service.BazService"
//  aggregate="true" optional="true" />
    public void testOptionalAndAggregate() {
        ExportedService svc = new ExportedService()
        .setSpecification("org.apache.felix.ipojo.test.composite.service.FooService")
        .setOptional(true)
        .setAggregate(true);
        
        
        Element elem = svc.getElement();
        String spec = elem.getAttribute("specification");
        String opt = elem.getAttribute("optional");
        String agg = elem.getAttribute("aggregate");


        assertEquals("spec", "org.apache.felix.ipojo.test.composite.service.FooService", spec);
        assertEquals("optional", "true", opt);
        assertEquals("aggregate", "true", agg);
    }

//  <comp:provides action="export"
//  specification="org.apache.felix.ipojo.test.composite.service.BazService"
//  filter="(instance.name=foo1)" />
    public void testFilter() {
        ExportedService svc = new ExportedService()
        .setSpecification("org.apache.felix.ipojo.test.composite.service.FooService")
        .setFilter("(&(int=2)(long=40))");
         
        Element elem = svc.getElement();
        String spec = elem.getAttribute("specification");
        String filter = elem.getAttribute("filter");

        assertEquals("spec", "org.apache.felix.ipojo.test.composite.service.FooService", spec);
        assertEquals("filter", "(&(int=2)(long=40))", filter);
    }

}
