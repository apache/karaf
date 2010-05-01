package org.apache.felix.ipojo.tests.core;

import org.apache.felix.ipojo.Factory;
import org.osgi.framework.ServiceReference;
import org.ow2.chameleon.testing.helpers.OSGiHelper;

public class Tools {


    /**
     * Get the Factory linked to the given pid
     * @param osgi
     * @param name
     * @return The factory
     */
    public static Factory getValidFactory(final OSGiHelper osgi, final String name) {
        // Get The Factory ServiceReference
        ServiceReference facref = osgi.getServiceReference(Factory.class.getName(), "(&(factory.state=1)(factory.name=" + name + "))");
        // Get the factory
        Factory factory = (Factory) osgi.getServiceObject(facref);

        return factory;
    }

}
