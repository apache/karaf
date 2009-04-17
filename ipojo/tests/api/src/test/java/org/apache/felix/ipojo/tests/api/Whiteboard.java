package org.apache.felix.ipojo.tests.api;

import org.apache.felix.ipojo.api.HandlerConfiguration;
import org.apache.felix.ipojo.metadata.Attribute;
import org.apache.felix.ipojo.metadata.Element;

public class Whiteboard implements HandlerConfiguration {
    
    public static final String NAME = "wbp";
    
    public static final String NAMESPACE = "org.apache.felix.ipojo.whiteboard";
    
    private String arrival;
    
    private String departure;
    
    private String modification;
    
    private String filter;
    
    public Whiteboard onArrival(String method) {
        arrival = method;
        return this;
    }
    
    public Whiteboard onDeparture(String method) {
        departure = method;
        return this;
    }
    
    public Whiteboard onModification(String method) {
        modification = method;
        return this;
    }
    
    public Whiteboard setFilter(String fil) {
        filter = fil;
        return this;
    }

    public Element getElement() {
        ensureValidity();
        // Create the root element.
        Element element = new Element(NAME, NAMESPACE);
        // Mandatory attributes
        element.addAttribute(new Attribute("onArrival", arrival));
        element.addAttribute(new Attribute("onDeparture", departure));
        element.addAttribute(new Attribute("filter", filter));
        
        // Optional attribute
        if (modification != null) {
            element.addAttribute(new Attribute("onModification", modification));
        }        
        
        return element;
    }

    private void ensureValidity() {
        if (arrival == null) {
            throw new IllegalStateException("The whiteboard pattern configuration must have a onArrival method");
        }
        if (departure == null) {
            throw new IllegalStateException("The whiteboard pattern configuration must have a onDeparture method");
        }
        if (filter == null) {
            throw new IllegalStateException("The whiteboard pattern configuration must have a filter");
        }
        
    }

}
