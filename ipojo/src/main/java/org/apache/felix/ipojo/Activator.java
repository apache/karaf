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
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.felix.ipojo.metadata.Element;
import org.apache.felix.ipojo.parser.ManifestMetadataParser;
import org.apache.felix.ipojo.parser.ParseException;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;


/**
 * iPOJO generic activator.
 * @author <a href="mailto:felix-dev@incubator.apache.org">Felix Project Team</a>
 */
public class Activator implements BundleActivator {

    // STATIC
    /**
     * The iPOJO static logger. This logger is used by each iPOJO instance.
     */
    private static Logger m_logger = Logger.getLogger("org.apache.felix.ipojo");

    /**
     * @return Returns the static ipojo logger : org.apache.felix.ipojo
     */
    public static Logger getLogger() { return Activator.m_logger; }
    // END STATIC

    // NON STATIC PART

    /**
     * The m_bundle context.
     * m_bundleContext : BundleContext
     */
    private BundleContext m_bundleContext = null;

    /**
     * The component manager for this activator.
     * m_handler : ComponentManagerFactory
     */
    private ComponentManagerFactory[] m_factories;

    /**
     * The configuration to create.
     * m_configuration : Array of Dictionary
     */
    private Dictionary[] m_configurations;

    // Field accessors  :

    /**
     * @return the m_bundle context
     */
    public BundleContext getBundleContext() {
        return m_bundleContext;
    }

    /**
     * Add a component manager factory to the factory list.
     * @param cm : the new component metadata.
     */
    public void addComponentFactory(Element cm) {
        // Create the factory :
        ComponentManagerFactory factory = new ComponentManagerFactory(m_bundleContext, cm);

        // If the factory array is not empty add the new factory at the end
        if (m_factories.length != 0) {
            ComponentManagerFactory[] newFactory = new ComponentManagerFactory[m_factories.length + 1];
            System.arraycopy(m_factories, 0, newFactory, 0, m_factories.length);
            newFactory[m_factories.length] = factory;
            m_factories = newFactory;
        }
        // Else create an array of size one with the new Factory
        else { m_factories = new ComponentManagerFactory[] {factory}; }
    }

    /**
     * Remove a component manager factory to the factory list.
     * @param factory : the componet facotry to remove
     */
    public void removeComponentFactory(ComponentManagerFactory factory) {

        int idx = -1;
        for (int i = 0; i < m_factories.length; i++) {
            if (m_factories[i] == factory) {
                idx = i;
                break;
            }
        }

        if (idx >= 0) {
            // If this is the factory, then point to empty list.
            if ((m_factories.length - 1) == 0) {
                m_factories = new ComponentManagerFactory[0];
            }
            // Otherwise, we need to do some array copying.
            else {
                ComponentManagerFactory[] newFactories = new ComponentManagerFactory[m_factories.length - 1];
                System.arraycopy(m_factories, 0, newFactories, 0, idx);
                if (idx < newFactories.length) {
                    System.arraycopy(m_factories, idx + 1, newFactories, idx, newFactories.length - idx);
                }
                m_factories = newFactories;
            }
        }
    }

    // End field accesors

    // Constructors :

    /**
     * Constructor used by Felix.
     */
    public Activator() {
        super();
        m_factories = new ComponentManagerFactory[0];
    }

    // End constuctors

    // Bundle Lifecycle CallBack

    /**
     * Start method.
     * @see org.osgi.framework.BundleActivator#start(org.osgi.framework.BundleContext)
     * @param bc : the m_bundle context to use to manage the component.
     * @throws Exception : when a problem occurs
     */
    public void start(final BundleContext bc) throws Exception {
        m_bundleContext = bc;
        Activator.getLogger().setLevel(IPojoConfiguration.LOG_LEVEL);

        try {
            parse();
        } catch (Exception e) {
            Activator.getLogger().log(Level.SEVERE, "Parse error for the bundle " + m_bundleContext.getBundle().getBundleId() + " : " + e.getMessage());
            return;
        }
        Activator.getLogger().log(Level.INFO, "[Bundle" + m_bundleContext.getBundle().getBundleId() + "] Called start after the parsing");

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
            ComponentManagerFactory factory = m_factories[i];
            factory.stop();
        }
    }

    // End Bundle Lifecycle CallBack

    // Parsing methods :

    /**
     * Parse the file who is at the Metadata-Location place, manipulate the bytecode of the component implementation class
     * and set the manager.
     * @throws IOException : the manisfest could not be found
     * @throws ParseException : the parsing process failed
     */
    private void parse() throws IOException, ParseException {

        String componentClasses = (String) m_bundleContext.getBundle().getHeaders().get("iPOJO-Components");
        if (componentClasses != null) {
            parseManifest(m_bundleContext.getBundle().getHeaders());
        } else {
            Activator.getLogger().log(Level.SEVERE, "[Bundle" + m_bundleContext.getBundle().getBundleId() + "] Components-Metadata are not in the manifest");
            throw new ParseException("[Bundle" + m_bundleContext.getBundle().getBundleId() + "] Component-Metadata are not in the manifest");
        }
    }

    /**
     * Parse the manifest.
     * @param dict : Manifest Entries
     * @throws ParseException : throwed when the parsing failed
     */
    private void parseManifest(Dictionary dict) throws ParseException {
        ManifestMetadataParser parser = new ManifestMetadataParser();
        parser.parse(dict);
        // Create the components Factory according to the declared component
        Element[] componentsMetadata = parser.getComponentsMetadata();
        m_configurations = parser.getInstances();
        for (int i = 0; i < componentsMetadata.length; i++) {
            Activator.getLogger().log(Level.INFO, "[Bundle" + m_bundleContext.getBundle().getBundleId() + "] Create a component factory for " + componentsMetadata[i].getAttribute("classname"));
            addComponentFactory(componentsMetadata[i]);
        }

    }

    /**
     * Start the management : Manipulate the classes and start the component manager.
     */
    private void start() {
        for (int i = 0; i < m_factories.length; i++) {
            ComponentManagerFactory factory = m_factories[i];
            String componentClass = factory.getComponentClassName();
            for (int j = 0; j < m_configurations.length; j++) {
                Dictionary conf = m_configurations[j];
                if (conf.get("component") != null && conf.get("component").equals(componentClass)) {
                    factory.createComponent(m_configurations[j]); // create a component
                }
            }
            factory.start(); // Start the factory exposition
        }
    }

}
