package org.apache.felix.ipojo.test.scenarios.manipulation;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

import org.apache.felix.ipojo.junit4osgi.OSGiTestCase;
import org.apache.felix.ipojo.test.scenarios.component.Marker;
import org.apache.felix.ipojo.test.scenarios.component.SubMarker;

public class Annotation extends OSGiTestCase {
    
    private Class clazz;
    
    public void setUp() {
        try {
            clazz = context.getBundle().
                loadClass("org.apache.felix.ipojo.test.scenarios.component.Annotation");
        } catch (ClassNotFoundException e) {
            fail("Cannot load the annotation class : " + e.getMessage());
        }
    }
    
    public void testAnnotationOnMethod() {
        Method method = null;
        try {
            method = this.clazz.getMethod("doSomething", new Class[0]);
        } catch (Exception e) {
            fail("Cannot find the doSomething method : " + e.getMessage());
        } 
        assertNotNull("Check method existence", method);
        
        java.lang.annotation.Annotation[] annotations = method.getDeclaredAnnotations();
        assertNotNull("Check annotations size - 1", annotations);
        assertEquals("Check annotations size - 2", 2, annotations.length); // Invisible is not visible
        
        /*
            @Marker(name="marker", type=Type.BAR, 
            sub=@SubMarker(subname="foo"),
            arrayOfObjects={"foo", "bar", "baz"},
            arrayOfAnnotations= {@SubMarker(subname="foo")}
            )
            @SubMarker(subname="bar")
            @Invisible
         */
        
        Marker marker = getMarkerAnnotation(annotations);
        assertNotNull("Check marker", marker);
        
        assertEquals("Check marker name", "marker", marker.name());
        assertEquals("Check marker type", Marker.Type.BAR, marker.type());
        assertEquals("Check sub marker attribute", "foo", marker.sub().subname());
        assertEquals("Check objects [0]", "foo", marker.arrayOfObjects()[0]);
        assertEquals("Check objects [1]", "bar", marker.arrayOfObjects()[1]);
        assertEquals("Check objects [2]", "baz", marker.arrayOfObjects()[2]);
        assertEquals("Check annotations[0]", "foo", marker.arrayOfAnnotations()[0].subname());
        
        SubMarker sub = getSubMarkerAnnotation(annotations);
        assertNotNull("Check submarker", sub);
        assertEquals("Check submarker", "bar", sub.subname());
        
    }
    
    public void testAnnotationOnConstructor() {
        Constructor method = null;
        try {
            method = clazz.getConstructor(new Class[0]);
        } catch (Exception e) {
            fail("Cannot find the constructor method : " + e.getMessage());
        } 
        assertNotNull("Check method existence", method);
        
        java.lang.annotation.Annotation[] annotations = method.getDeclaredAnnotations();
        assertNotNull("Check annotations size - 1", annotations);
        assertEquals("Check annotations size - 2", 2, annotations.length); // Invisible is not visible
        
        /*
            @Marker(name="marker", type=Type.BAR, 
            sub=@SubMarker(subname="foo"),
            arrayOfObjects={"foo", "bar", "baz"},
            arrayOfAnnotations= {@SubMarker(subname="foo")}
            )
            @SubMarker(subname="bar")
            @Invisible
         */
        
        Marker marker = getMarkerAnnotation(annotations);
        assertNotNull("Check marker", marker);
        
        assertEquals("Check marker name", "marker", marker.name());
        assertEquals("Check marker type", Marker.Type.BAR, marker.type());
        assertEquals("Check sub marker attribute", "foo", marker.sub().subname());
        assertEquals("Check objects [0]", "foo", marker.arrayOfObjects()[0]);
        assertEquals("Check objects [1]", "bar", marker.arrayOfObjects()[1]);
        assertEquals("Check objects [2]", "baz", marker.arrayOfObjects()[2]);
        assertEquals("Check annotations[0]", "foo", marker.arrayOfAnnotations()[0].subname());
        
        SubMarker sub = getSubMarkerAnnotation(annotations);
        assertNotNull("Check submarker", sub);
        assertEquals("Check submarker", "bar", sub.subname());
    }
    
    private Marker getMarkerAnnotation(java.lang.annotation.Annotation[] annotations) {
        for (int i = 0; i < annotations.length; i++) {
            if (annotations[i].annotationType().getName().equals("org.apache.felix.ipojo.test.scenarios.component.Marker")) {
                return (Marker) annotations[i];
            }
        }
        return null;
    }
    
    private SubMarker getSubMarkerAnnotation(java.lang.annotation.Annotation[] annotations) {
        for (int i = 0; i < annotations.length; i++) {
            if (annotations[i].annotationType().getName().equals("org.apache.felix.ipojo.test.scenarios.component.SubMarker")) {
                return (SubMarker) annotations[i];
            }
        }
        return null;
    }

}
