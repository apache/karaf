package org.apache.felix.ipojo.api.composite;


import junit.framework.TestCase;

import org.apache.felix.ipojo.metadata.Element;

public class ImportedServiceTest extends TestCase {
    
//    <comp:composite name="composite.requires.1" architecture="true">
//    <subservice action="import"
//        specification="org.apache.felix.ipojo.test.composite.service.FooService"
//        scope="composite" />
//</comp:composite>
    public void testSimple() {
        ImportedService svc = new ImportedService()
        .setSpecification("org.apache.felix.ipojo.test.composite.service.FooService");
        
        Element elem = svc.getElement();
        String spec = elem.getAttribute("specification");
        assertEquals("spec", "org.apache.felix.ipojo.test.composite.service.FooService", spec);
    }
    
    public void testBad() {
        ImportedService svc = new ImportedService()
            //.setSpecification("org.apache.felix.ipojo.test.composite.service.BarService") NO SPEC
            ;
        try {
            svc.getElement();
            fail("Invalid element accepted");
        } catch (IllegalStateException e) {
            // OK
        }
    }
    
    public void testScope() {
        ImportedService svc = new ImportedService()
        .setSpecification("org.apache.felix.ipojo.test.composite.service.FooService")
        .setScope(ImportedService.COMPOSITE_SCOPE);
        
        
        Element elem = svc.getElement();
        String spec = elem.getAttribute("specification");
        String scope = elem.getAttribute("scope");
        assertEquals("spec", "org.apache.felix.ipojo.test.composite.service.FooService", spec);
        assertEquals("scope", "composite", scope);

    }
    
//
//<comp:composite name="composite.requires.2" architecture="true">
//    <subservice action="import"
//        specification="org.apache.felix.ipojo.test.composite.service.FooService"
//        aggregate="true" scope="composite" />
//</comp:composite>
    public void testAggregate() {
        ImportedService svc = new ImportedService()
        .setSpecification("org.apache.felix.ipojo.test.composite.service.FooService")
        .setScope(ImportedService.COMPOSITE_SCOPE)
        .setAggregate(true);
        
        
        Element elem = svc.getElement();
        String spec = elem.getAttribute("specification");
        String scope = elem.getAttribute("scope");
        String agg = elem.getAttribute("aggregate");

        assertEquals("spec", "org.apache.felix.ipojo.test.composite.service.FooService", spec);
        assertEquals("scope", "composite", scope);
        assertEquals("aggregate", "true", agg);
    }
    
//
//<comp:composite name="composite.requires.3" architecture="true">
//    <subservice action="import"
//        specification="org.apache.felix.ipojo.test.composite.service.FooService"
//        optional="true" scope="composite" />
//</comp:composite>
    public void testOptional() {
        ImportedService svc = new ImportedService()
        .setSpecification("org.apache.felix.ipojo.test.composite.service.FooService")
        .setScope(ImportedService.COMPOSITE_SCOPE)
        .setOptional(true);
        
        
        Element elem = svc.getElement();
        String spec = elem.getAttribute("specification");
        String scope = elem.getAttribute("scope");
        String opt = elem.getAttribute("optional");

        assertEquals("spec", "org.apache.felix.ipojo.test.composite.service.FooService", spec);
        assertEquals("scope", "composite", scope);
        assertEquals("optional", "true", opt);
    }
//
//<comp:composite name="composite.requires.4" architecture="true">
//    <subservice action="import"
//        specification="org.apache.felix.ipojo.test.composite.service.FooService"
//        optional="true" aggregate="true" scope="comp:composite" />
//</comp:composite>
    public void testOptionalAndAggregate() {
        ImportedService svc = new ImportedService()
        .setSpecification("org.apache.felix.ipojo.test.composite.service.FooService")
        .setScope(ImportedService.COMPOSITE_SCOPE)
        .setOptional(true)
        .setAggregate(true);
        
        
        Element elem = svc.getElement();
        String spec = elem.getAttribute("specification");
        String scope = elem.getAttribute("scope");
        String opt = elem.getAttribute("optional");
        String agg = elem.getAttribute("aggregate");


        assertEquals("spec", "org.apache.felix.ipojo.test.composite.service.FooService", spec);
        assertEquals("scope", "composite", scope);
        assertEquals("optional", "true", opt);
        assertEquals("aggregate", "true", agg);
    }
//
//<comp:composite name="composite.requires.5" architecture="true">
//    <subservice action="import"
//        specification="org.apache.felix.ipojo.test.composite.service.FooService"
//        filter="(&amp;(int=2)(long=40))" scope="composite" />
//</comp:composite>
    public void testFilter() {
        ImportedService svc = new ImportedService()
        .setSpecification("org.apache.felix.ipojo.test.composite.service.FooService")
        .setScope(ImportedService.COMPOSITE_SCOPE)
        .setFilter("(&(int=2)(long=40))");
         
        Element elem = svc.getElement();
        String spec = elem.getAttribute("specification");
        String scope = elem.getAttribute("scope");
        String filter = elem.getAttribute("filter");

        assertEquals("spec", "org.apache.felix.ipojo.test.composite.service.FooService", spec);
        assertEquals("scope", "composite", scope);
        assertEquals("filter", "(&(int=2)(long=40))", filter);
    }

}
