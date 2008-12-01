package org.apache.felix.ipojo.test.scenarios.annotations;

import org.apache.felix.ipojo.junit4osgi.OSGiTestCase;
import org.apache.felix.ipojo.junit4osgi.helpers.IPOJOHelper;
import org.apache.felix.ipojo.metadata.Element;

public class EventAdmin extends OSGiTestCase {
    String type = "org.apache.felix.ipojo.test.scenarios.component.event.PubSub";
    String namespace = "org.apache.felix.ipojo.handlers.event";
    
    Element component;
    private IPOJOHelper helper;
    
    public void setUp() {
        helper = new IPOJOHelper(this);
        component = helper.getMetadata(type);
        assertNotNull("Check component", component);
    }
    
    public void tearDown() {
        component = null;
    }
    
    public void testP1() {
        //P1, synchronous
        Element elem = getElementByName("p1");
        checkPublisher(elem);
        assertNull("Check topics", elem.getAttribute("topics"));
        assertEquals("Check synchronous", "true", elem.getAttribute("synchronous"));
        assertEquals("Check field", "publisher1", elem.getAttribute("field"));
        assertNull("Check data_key", elem.getAttribute("data_key"));  
    }
    
    public void testP2() {
        //name="p2", synchronous=false, topics="foo,bar", data_key="data"
        Element elem = getElementByName("p2");
        checkPublisher(elem);
        assertEquals("Check topics (" + elem.getAttribute("topics")+")", "foo,bar", elem.getAttribute("topics"));
        assertEquals("Check synchronous", "false", elem.getAttribute("synchronous"));
        assertEquals("Check field", "publisher2", elem.getAttribute("field"));
        assertEquals("Check data_key", "data" ,elem.getAttribute("data_key"));  
    }
    
    public void testP3() {
        //name="p3", synchronous=true, topics="bar"
        Element elem = getElementByName("p3");
        checkPublisher(elem);
        assertEquals("Check topics", "bar" ,elem.getAttribute("topics"));
        assertEquals("Check synchronous", "true", elem.getAttribute("synchronous"));
        assertEquals("Check field", "publisher3", elem.getAttribute("field"));
        assertNull("Check data_key", elem.getAttribute("data_key"));  
    }
    
    public void testS1() {
        //name="s1", data_key="data"
        Element elem = getElementByName("s1");
        checkSubscriber(elem);
        assertNull("Check topics",elem.getAttribute("topics"));
        assertEquals("Check method", "receive1", elem.getAttribute("method"));
        assertEquals("Check data_key", "data" ,elem.getAttribute("data_key"));  
        assertNull("Check data_type", elem.getAttribute("data_type"));
        assertNull("Check filter", elem.getAttribute("filter"));
    }
    
    public void testS2() {
        //name="s2", topics="foo,bar", filter="(foo=true)"
        Element elem = getElementByName("s2");
        checkSubscriber(elem);
        assertEquals("Check topics", "foo,bar", elem.getAttribute("topics"));
        assertEquals("Check method", "receive2", elem.getAttribute("method"));
        assertNull("Check data_key" ,elem.getAttribute("data_key"));  
        assertNull("Check data_type", elem.getAttribute("data_type"));
        assertEquals("Check filter","(foo=true)" , elem.getAttribute("filter"));
    }
    
    public void testS3() {
        //name="s3", topics="foo", data_key="data", data_type="java.lang.String"
        Element elem = getElementByName("s3");
        checkSubscriber(elem);
        assertEquals("Check topics", "foo", elem.getAttribute("topics"));
        assertEquals("Check method", "receive3", elem.getAttribute("method"));
        assertEquals("Check data_key", "data" ,elem.getAttribute("data_key"));  
        assertEquals("Check data_type", "java.lang.String", elem.getAttribute("data_type"));
        assertNull("Check filter", elem.getAttribute("filter"));
    }
    
   
    
    public Element getElementByName(String name) {
        Element [] elems = component.getElements();
        for (int i = 0; i < elems.length; i++) {
            if (elems[i].containsAttribute("name") && elems[i].getAttribute("name").equals(name)) {
                return elems[i];
            }
        }
        return null;
    }
    
    public void checkSubscriber(Element elem) {
        assertNotNull("Can't check subscriber : null element",elem);
        String ns = elem.getNameSpace();
        String nm = elem.getName();
        assertEquals("Elem is not a subscriber : bad namespace", namespace, ns);
        assertEquals("Elem is not a subscriber : bad name", "subscriber", nm);

    }
    
    public void checkPublisher(Element elem) {
        assertNotNull("Can't check publisher : null element",elem);
        String ns = elem.getNameSpace();
        String nm = elem.getName();
        assertEquals("Elem is not a publisher : bad namespace", namespace, ns);
        assertEquals("Elem is not a publisher : bad name", "publisher", nm);
    }

}
