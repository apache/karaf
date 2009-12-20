package org.apache.felix.ipojo.test.scenarios.annotations;

import java.util.ArrayList;
import java.util.List;

import org.apache.felix.ipojo.junit4osgi.OSGiTestCase;
import org.apache.felix.ipojo.junit4osgi.helpers.IPOJOHelper;
import org.apache.felix.ipojo.metadata.Element;
import org.apache.felix.ipojo.parser.ManifestMetadataParser;
import org.apache.felix.ipojo.parser.ParseException;
import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceReference;

public class Instantiate extends OSGiTestCase {

    private IPOJOHelper helper;

    public void setUp() {
        helper = new IPOJOHelper(this);
    }

    public void testInstantiateSimple() {
        Element[] meta = getInstanceMetadata(context.getBundle(), "org.apache.felix.ipojo.test.scenarios.component.Instantiate");
        assertNotNull(meta);
        assertEquals(1, meta.length);
        assertNull(meta[0].getAttribute("name"));
        assertEquals(0, meta[0].getElements().length);
    }
    
    public void testInstantiateWithName() {
        // Check instance tag
        Element[] meta = getInstanceMetadata(context.getBundle(), "org.apache.felix.ipojo.test.scenarios.component.InstantiateWithName");
        assertNotNull(meta);
        assertEquals(1, meta.length);
        assertNull(meta[0].getAttribute("name"));
        assertEquals(0, meta[0].getElements().length);
    }
    
    public void testInstanceCreation() {
        String in = "org.apache.felix.ipojo.test.scenarios.component.Instantiate-0";
        ServiceReference ref = helper.getServiceReferenceByName(Architecture.class.getName(), in);
        assertNotNull(ref);
    }
    
    public void testInstanceCreationWithName() {
        String in = "myInstantiatedInstance";
        ServiceReference ref = helper.getServiceReferenceByName(Architecture.class.getName(), in);
        assertNotNull(ref);
    }

    /**
     * Returns the instance metadatas of the component with the given name,
     * defined in the given bundle.
     * 
     * @param bundle the bundle from which the component is defined.
     * @param component the name of the defined component.
     * @return the list of instance metadata of the component with the given name,
     *         defined in the given bundle, or {@code null} if not found.
     */
    public static Element[] getInstanceMetadata(Bundle bundle, String component) {

        // Retrieves the component description from the bundle's manifest.
        String elem = (String) bundle.getHeaders().get("iPOJO-Components");
        if (elem == null) {
            throw new IllegalArgumentException(
                    "Cannot find iPOJO-Components descriptor in the specified bundle ("
                            + bundle.getSymbolicName()
                            + "). Not an iPOJO bundle.");
        }

        // Parses the retrieved description and find the component with the
        // given name.
        List list = new ArrayList();
        try {
            Element element = ManifestMetadataParser.parseHeaderMetadata(elem);
            Element[] childs = element.getElements("instance");
            for (int i = 0; i < childs.length; i++) {
                String name = childs[i].getAttribute("component");
                if (name != null && name.equalsIgnoreCase(component)) {
                    list.add(childs[i]);
                }
            }
            
            if (list.isEmpty()) {
                // Component not found...
                return null;
            } else {
                return (Element[]) list.toArray(new Element[list.size()]);
            }

        } catch (ParseException e) {
            throw new IllegalStateException(
                    "Cannot parse the components from specified bundle ("
                            + bundle.getSymbolicName() + "): " + e.getMessage());
        }
    }

}

