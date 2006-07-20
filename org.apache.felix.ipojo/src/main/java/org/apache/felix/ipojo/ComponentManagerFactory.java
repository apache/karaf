/*
 *   Copyright 2006 The Apache Software Foundation
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 */
package org.apache.felix.ipojo;

import java.io.IOException;
import java.net.URL;
import java.security.ProtectionDomain;
import java.util.Enumeration;
import java.util.logging.Level;

import org.apache.felix.ipojo.metadata.Element;
import org.osgi.framework.BundleContext;

/**
 * The component manager factory class manages component manager object.
 * @author <a href="mailto:felix-dev@incubator.apache.org">Felix Project Team</a>
 */
public class ComponentManagerFactory {

	// Fields :
	/**
	 * List of the managed component manager.
	 */
	private ComponentManager[] m_componentManagers = new ComponentManager[0];

	/**
	 * The bundle context reference.
	 */
	private BundleContext m_bundleContext = null;

	/**
	 * Component class.
	 */
	private byte[] m_clazz = null;

	/**
	 * Component Class Name.
	 */
	private String m_componentClassName = null;

	/**
	 * Classloader to delegate loading.
	 */
	private FactoryClassloader m_classLoader = null;

	//End field

	/**
	 * FactoryClassloader.
	 */
	private class FactoryClassloader extends ClassLoader {

	    /**
	     * load the class.
	     * @see java.lang.ClassLoader#loadClass(java.lang.String, boolean)
	     * @param name : the name of the class
	     * @param resolve : should be the class resolve now ?
	     * @return : the loaded class
	     * @throws ClassNotFoundException : the class to load is not found
	     */
	    protected synchronized Class loadClass(final String name,
	            final boolean resolve) throws ClassNotFoundException {
	       return m_bundleContext.getBundle().loadClass(name);
	    }


	    /**
	     * Return the URL of the asked ressource.
	     * @param arg : the name of the resource to find.
	     * @return the URL of the resource.
	     * @see java.lang.ClassLoader#getResource(java.lang.String)
	     */
	    public URL getResource(String arg) {
	        return m_bundleContext.getBundle().getResource(arg);
	    }

	    /**
	     * .
	     * @param arg : resource to find
	     * @return : the enumeration found
	     * @throws IOException : if the lookup failed.
	     * @see java.lang.ClassLoader#getResources(java.lang.String)
	     */
	    public Enumeration getRessources(String arg) throws IOException {
	        return m_bundleContext.getBundle().getResources(arg);
	    }

	    /**
	     * The defineClass method.
	     * @param name : name of the class
	     * @param b : the byte array of the class
	     * @param domain : the protection domain
	     * @return : the defined class.
	     * @throws Exception : if a problem is detected during the loading
	     */
	    public Class defineClass(String name, byte[] b,
	            ProtectionDomain domain) throws Exception {
	    	return super.defineClass(name, b, 0, b.length, domain);
	    }
	}

	// Field accessors

	 /**
     * Add a component manager factory to the component manager list.
     * @param cm : the new component metadata.
     */
    private void addComponent(ComponentManager cm) {

    	// If the component manager array is not empty add the new factory at the end
        if (m_componentManagers.length != 0) {
        	ComponentManager[] newCM = new ComponentManager[m_componentManagers.length + 1];
            System.arraycopy(m_componentManagers, 0, newCM, 0, m_componentManagers.length);
            newCM[m_componentManagers.length] = cm;
            m_componentManagers = newCM;
        }
        // Else create an array of size one with the new component manager
        else {
            m_componentManagers = new ComponentManager[] {cm};
        }
    }

    /**
     * Remove the component manager for m the list.
     * @param cm : the component manager to remove
     */
    public void removeComponent(ComponentManager cm) {
    	cm.stop();
    	int idx = -1;

    	for (int i = 0; i < m_componentManagers.length; i++) {
    		if (m_componentManagers[i] == cm) { idx = i; }
    	}

        if (idx >= 0) {
            if ((m_componentManagers.length - 1) == 0) { m_componentManagers = new ComponentManager[0]; }
            else {
            	ComponentManager[] newCMList = new ComponentManager[m_componentManagers.length - 1];
                System.arraycopy(m_componentManagers, 0, newCMList, 0, idx);
                if (idx < newCMList.length) {
                    System.arraycopy(m_componentManagers, idx + 1, newCMList, idx, newCMList.length - idx); }
                m_componentManagers = newCMList;
            }
            }
       }

    /**
     * @return the iPOJO activator reference
     */
    public BundleContext getBundleContext() { return m_bundleContext; }

	// End field accessors

	/**
	 * Create a component manager factory and create a component manager with the given medatada.
	 * @param bc : bundle context
	 * @param cm : metadata of the component to create
	 */
	public ComponentManagerFactory(BundleContext bc, Element cm) {
		m_bundleContext = bc;
		createComponentManager(cm);
		m_componentClassName = cm.getAttribute("className");
	}

	/**
	 * Create a component manager factory and create a component manager with the given medatada.
	 * @param bc : bundle context
	 * @param clazz : the component class
	 * @param cm : metadata of the component
	 */
	public ComponentManagerFactory(BundleContext bc, byte[] clazz, Element cm) {
		m_bundleContext = bc;
		m_clazz = clazz;
		m_componentClassName = cm.getAttribute("className");
		createComponentManager(cm);
	}

	/**
	 * Create a component manager factory, no component manager are created.
	 * @param bc
	 */
	public ComponentManagerFactory(BundleContext bc) {
		m_bundleContext = bc;
	}

	/**
	 * Create a component manager form the component metadata.
	 * @param cm : Component Metadata
	 * @return a component manager configured with the metadata
	 */
	public ComponentManager createComponentManager(Element cm) {
		ComponentManager component = new ComponentManager(this);
		component.configure(cm);
		addComponent(component);
		return component;
	}

	// Factory lifecycle management

	/**
	 * Stop all the component managers.
	 */
	public void stop() {
		Activator.getLogger().log(Level.INFO, "[Bundle " + m_bundleContext.getBundle().getBundleId() + "] Stop the component factory");
		for (int i = 0; i < m_componentManagers.length; i++) {
			ComponentManager cm = m_componentManagers[i];
			cm.stop();
		}
	}

	/**
	 * Start all the component managers.
	 */
	public void start() {
		Activator.getLogger().log(Level.INFO, "[Bundle " + m_bundleContext.getBundle().getBundleId() + "] Start the component factory");
		for (int i = 0; i < m_componentManagers.length; i++) {
			ComponentManager cm = m_componentManagers[i];
			cm.start();
		}
	}

	/**
	 * Load a class.
	 * @param className : name of the class to load
	 * @return the resulting Class object
	 * @throws ClassNotFoundException : happen when the class is not found
	 */
	public Class loadClass(String className) throws ClassNotFoundException {
		Activator.getLogger().log(Level.INFO, "[Bundle " + m_bundleContext.getBundle().getBundleId() + "] In load for : " + className);
		if (m_clazz != null && className.equals(m_componentClassName)) {
			if (m_classLoader == null) {
				Activator.getLogger().log(Level.INFO, "[Bundle " + m_bundleContext.getBundle().getBundleId() + "] Create the FactoryClassLoader for : " + className);
				m_classLoader = new FactoryClassloader();
				}
			try {
				Class c = m_classLoader.defineClass(m_componentClassName, m_clazz, null);
				Activator.getLogger().log(Level.INFO, "[Bundle " + m_bundleContext.getBundle().getBundleId() + "] Return " + c + " for " + className);
				return c;
			} catch (Exception e) {
				Activator.getLogger().log(Level.SEVERE, "[Bundle " + m_bundleContext.getBundle().getBundleId() + "] Cannot define the class : " + className);
				return null;
			}
		}
		return m_bundleContext.getBundle().loadClass(className);
	}

	/**
	 * Return the URL of a resource.
	 * @param resName : resource name
	 * @return the URL of the resource
	 */
	public URL getResource(String resName) {
		return m_bundleContext.getBundle().getResource(resName);
	}

}
