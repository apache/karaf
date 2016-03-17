package org.apache.karaf.event.service;

import java.util.Dictionary;
import java.util.Hashtable;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.service.event.EventHandler;

public class Activator implements BundleActivator {

    @Override
    public void start(BundleContext context) throws Exception {
        EventCollector collector = new EventCollector();
        Dictionary<String, String> props = new Hashtable<>();
        props.put("event.topics", "*");
        String[] ifAr = new String[]{EventHandler.class.getName(), EventCollector.class.getName()};
        context.registerService(ifAr, collector, props);
    }

    @Override
    public void stop(BundleContext context) throws Exception {
    }

}
