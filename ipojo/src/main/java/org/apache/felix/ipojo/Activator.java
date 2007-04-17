/* 
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.felix.ipojo;

import java.io.IOException;
import java.util.Dictionary;

import org.apache.felix.ipojo.metadata.Element;
import org.apache.felix.ipojo.parser.ManifestMetadataParser;
import org.apache.felix.ipojo.parser.ParseException;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;


/**
 * iPOJO generic activator. 
 * All iPOJO bundle (bundle containing iPOJO components) must used this activator to be able to start the component management.
 * @author <a href="mailto:felix-dev@incubator.apache.org">Felix Project Team</a>
 */
public class Activator implements BundleActivator {
    /**
     * The m_bundle context.
     * m_bundleContext : BundleContext
     */
    private BundleContext m_bundleContext = null;

    /**
     * Component Factories managed by the current bundle.
     * m_handler : ComponentFactory
     */
    private ComponentFactory[] m_factories = new ComponentFactory[0];
    
    /**
     * The instance creator aims to manage instance from outside factory.
     */
    private InstanceCreator m_creator;

    /**
     * The configurations to create.
     * m_configuration : Array of Dictionary (each dictionary represents one configuration)
     */
    private Dictionary[] m_configurations;

    /**
     * Return the bundle context.
     * @return the bundle context.
     */
    BundleContext getBundleContext() { return m_bundleContext; }

    /**
     * Add a component factory to the factory list.
     * @param cm : the new component metadata.
     */
    private void addComponentFactory(Element cm) {
        ComponentFactory factory = new ComponentFactory(m_bundleContext, cm);

        // If the factory array is not empty add the new factory at the end
        if (m_factories.length != 0) {
            ComponentFactory[] newFactory = new ComponentFactory[m_factories.length + 1];
            System.arraycopy(m_factories, 0, newFactory, 0, m_factories.length);
            newFactory[m_factories.length] = factory;
            m_factories = newFactory;
        } else { m_factories = new ComponentFactory[] {factory}; }         // Else create an array of size one with the new Factory
    }

    /**
     * Start method.
     * @see org.osgi.framework.BundleActivator#start(org.osgi.framework.BundleContext)
     * @param bc : the m_bundle context to use to manage the component.
     * @throws Exception : when a problem occurs
     */
    public void start(final BundleContext bc) throws Exception {
        m_bundleContext = bc;

        try {
            parse();
        } catch (IOException e) {
            System.err.println("IO error for the bundle " + m_bundleContext.getBundle().getBundleId() + " : " + e.getMessage());
            return;
        } catch (ParseException e) {
            System.err.println("Parse error for the bundle " + m_bundleContext.getBundle().getBundleId() + " : " + e.getMessage());
        }

        start(); // Call the internal start method

    }

    /**
     * Stop method.
     * @see org.osgi.framework.BundleActivator#stop(org.osgi.framework.BundleContext)
     * @param arg0 : the m_bundle context
     * @throws Exception : ???
     */
    public void stop(BundleContext arg0) throws Exception {
        for (int i = 0; i < m_factories.length; i++) {
            ComponentFactory factory = m_factories[i];
            factory.stop();
        }
        if (m_creator != null) { m_creator.stop(); }
        m_factories = new ComponentFactory[0]; // Release all factories
    }

    /**
     * Parse the internal metadata (from the manifest (in the iPOJO-Components property)).
     * @throws IOException : the manisfest could not be found
     * @throws ParseException : the parsing process failed
     */
    private void parse() throws IOException, ParseException {

        String componentClasses = (String) m_bundleContext.getBundle().getHeaders().get("iPOJO-Components");
        if (componentClasses != null) {
            ManifestMetadataParser parser = new ManifestMetadataParser();
            parser.parse(m_bundleContext.getBundle().getHeaders());
            
            Element[] componentsMetadata = parser.getComponentsMetadata(); // Get the component type declaration
            for (int i = 0; i < componentsMetadata.length; i++) { addComponentFactory(componentsMetadata[i]); }
            m_configurations = parser.getInstances(); // Get the component instances declaration
        } else {
            throw new ParseException("[Bundle" + m_bundleContext.getBundle().getBundleId() + "] iPOJO-Components are not in the manifest");
        }
    }

    /**
     * Start the management factories and create instances.
     */
    private void start() {
        // Start the factories
        for (int j = 0; j < m_factories.length; j++) {
            m_factories[j].start();
        }

        Dictionary[] outsiders = new Dictionary[0];
        for (int i = 0; i < m_configurations.length; i++) {
            Dictionary conf = m_configurations[i];
            boolean created = false;
            for (int j = 0; j < m_factories.length; j++) {
                String componentClass = m_factories[j].getComponentClassName();
                String factoryName = m_factories[j].getName();
                if (conf.get("component") != null && (conf.get("component").equals(componentClass) || conf.get("component").equals(factoryName))) {
                    try {
                        m_factories[j].createComponentInstance(conf);
                        created = true;
                    } catch (UnacceptableConfiguration e) {
                        System.err.println("Cannot create the instance " + conf.get("name") + " : " + e.getMessage());
                    }
                }
            }
            if (!created && conf.get("component") != null) {
                if (outsiders.length != 0) {
                    Dictionary[] newList = new Dictionary[outsiders.length + 1];
                    System.arraycopy(outsiders, 0, newList, 0, outsiders.length);
                    newList[outsiders.length] = conf;
                    outsiders = newList;
                } else {
                    outsiders = new Dictionary[] { conf };
                }

            }
        }

        // Create the instance creator
        m_creator = new InstanceCreator(m_bundleContext, outsiders);
    }

}
