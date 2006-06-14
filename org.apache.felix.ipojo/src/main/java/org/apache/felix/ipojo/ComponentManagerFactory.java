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

import java.util.logging.Level;

import org.apache.felix.ipojo.metadata.Element;
import org.osgi.framework.BundleContext;

/**
 * The component manager factory class manages component manager object.
 * @author Clement Escoffier
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

	//End field

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
	 * Constructor of a ComponentManagerFactory from a component metadata.
	 * This contructor is use when the iPOJO Activator is used.
	 * @param cm : Component Metadata for the component factory
	 */
	protected ComponentManagerFactory(Activator activator, Element cm) {
		m_bundleContext = activator.getBundleContext();
		createComponentManager(cm);
	}

	/**
	 * Create a component manager factory and create a component manager with the given medatada.
	 * @param bc : bundle context
	 * @param cm : metadata of the component to create
	 */
	public ComponentManagerFactory(BundleContext bc, Element cm) {
		m_bundleContext = bc;
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

}
