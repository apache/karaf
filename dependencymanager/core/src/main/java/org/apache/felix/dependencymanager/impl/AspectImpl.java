package org.apache.felix.dependencymanager.impl;

import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;

import org.apache.felix.dependencymanager.DependencyManager;
import org.apache.felix.dependencymanager.Service;
import org.osgi.framework.ServiceReference;

public class AspectImpl {
	private volatile DependencyManager m_manager;
	private volatile Service m_service;
	private final Class m_serviceInterface;
	private final String m_serviceFilter;
	private final Object m_aspectImplementation;
	private final Map m_services = new HashMap();
    private final Dictionary m_aspectProperties;
	
	public AspectImpl(Class serviceInterface, String serviceFilter, Object aspectImplementation, Dictionary properties) {
		m_serviceInterface = serviceInterface;
		m_serviceFilter = serviceFilter;
		m_aspectImplementation = aspectImplementation;
		m_aspectProperties = properties;
	}

	public void added(ServiceReference ref, Object service) {
		Properties props = new Properties();
		String[] keys = ref.getPropertyKeys();
		for (int i = 0; i < keys.length; i++) {
		    props.put(keys[i], ref.getProperty(keys[i]));
		}
		Enumeration e = m_aspectProperties.keys();
        while (e.hasMoreElements()) {
            Object key = e.nextElement();
            props.put(key, m_aspectProperties.get(key));
        }

		Service newService = m_manager.createService()
			.setInterface(m_serviceInterface.getName(), props)
			.setImplementation(m_aspectImplementation)
			.add(m_service.getDependencies())
			.add(m_manager.createServiceDependency()
				.setService(m_serviceInterface, ref)
				.setRequired(true)
				);
		m_services.put(ref, newService);
		m_manager.add(newService);
	}

	public void removed(ServiceReference ref, Object service) {
		Service newService = (Service) m_services.remove(ref);
		if (newService == null) {
			System.out.println("Service should not be null here, dumping stack.");
			Thread.dumpStack();
		}
		else {
			m_manager.remove(newService);
		}
	}
	
    public void stop() { 
        Iterator i = m_services.values().iterator();
        while (i.hasNext()) {
            m_manager.remove((Service) i.next());
        }
        m_services.clear();
    }
}
