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
import java.util.Enumeration;
import java.util.Hashtable;

import org.apache.felix.ipojo.metadata.Element;
import org.apache.felix.ipojo.parser.ManifestMetadataParser;
import org.apache.felix.ipojo.parser.ParseException;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.SynchronousBundleListener;
import org.osgi.service.log.LogService;

/**
 * iPOJO Extender.
 * Looks for iPOJO Bundle and start the management of these bundles if needed.
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class Extender implements SynchronousBundleListener, BundleActivator {

    /**
     * iPOJO Manifest header.
     */
    private static final String IPOJO_HEADER = "iPOJO-Components";
    
    /**
     * iPOJO Bundle Context.
     */
    private BundleContext m_context;
    
    /**
     * Dictionary of [BundleId, Factory List]. 
     */
    private Dictionary m_components;
    
    /**
     * Dictionary of [BundleId, Instance Creator]. 
     */
    private Dictionary m_creators;
    
    /**
     * iPOJO Bundle Id.
     */
    private long m_bundleId;
    
    
    /**
     * Bundle Listener Notification.
     * @param event : the bundle event.
     * @see org.osgi.framework.BundleListener#bundleChanged(org.osgi.framework.BundleEvent)
     */
    public void bundleChanged(BundleEvent event) {
        if (event.getBundle().getBundleId() == m_bundleId) {
            return;
        }

        switch (event.getType()) {
            case BundleEvent.STARTED:
                startManagementFor(event.getBundle());
                break;
            case BundleEvent.STOPPING:
                closeManagementFor(event.getBundle());
                break;
            default: 
                break;
        }

    }

    /**
     * Ends the iPOJO Management for the given bundle.
     * @param bundle : the bundle.
     */
    private void closeManagementFor(Bundle bundle) {
        ComponentFactory[] cfs = (ComponentFactory[]) m_components.get(bundle);
        InstanceCreator creator = (InstanceCreator) m_creators.get(bundle);
        if (cfs == null && creator == null) { return; }
        for (int i = 0; cfs != null && i < cfs.length; i++) {
            ComponentFactory factory = cfs[i];
            factory.stop();
        }
        if (creator != null) { creator.stop(); }
        
        m_components.remove(bundle);
        m_creators.remove(bundle);
        
    }

    /**
     * Check if the given bundle is an iPOJO bundle, and begin the iPOJO management is true. 
     * @param bundle : the bundle to check.
     */
    private void startManagementFor(Bundle bundle) {
        // Check bundle
        Dictionary dict = bundle.getHeaders();
        String header = (String) dict.get(IPOJO_HEADER);
        if (header == null) {
            return;
        } else {
            try {
                parse(bundle, header);
            } catch (IOException e) {
                err("An exception occurs during the parsing of the bundle " + bundle.getBundleId(), e);
            } catch (ParseException e) {
                err("A parse exception occurs during the parsing of the bundle " + bundle.getBundleId(), e);
            }
        }
        
    }
    
    /**
     * Parse the internal metadata (from the manifest (in the iPOJO-Components property)).
     * @param bundle : the owner bundle.
     * @param components : iPOJO Header String.
     * @throws IOException : the manifest could not be found
     * @throws ParseException : the parsing process failed
     */
    private void parse(Bundle bundle, String components) throws IOException, ParseException {
        ManifestMetadataParser parser = new ManifestMetadataParser();
        parser.parseHeader(components);
          
        Element[] componentsMetadata = parser.getComponentsMetadata(); // Get the component type declaration
        for (int i = 0; i < componentsMetadata.length; i++) { addComponentFactory(bundle, componentsMetadata[i]); }
        
        start(bundle, parser.getInstances());
    }

    /** 
     * iPOJO Starting method.
     * @param bc : iPOJO bundle context.
     * @throws Exception : the start method failed.
     * @see org.osgi.framework.BundleActivator#start(org.osgi.framework.BundleContext)
     */
    public void start(BundleContext bc) throws Exception {
        m_context = bc;
        m_bundleId = bc.getBundle().getBundleId();
        m_components = new Hashtable();
        m_creators = new Hashtable();
        
        synchronized (this) {
            for (int i = 0; i < bc.getBundles().length; i++) {
                if (bc.getBundles()[i].getState() == Bundle.ACTIVE) {
                    startManagementFor(bc.getBundles()[i]);
                }
            }
        }

        // listen to any changes in bundles
        m_context.addBundleListener(this);
    }

    /**
     * Stop the iPOJO Management.
     * @param bc : bundle context.
     * @throws Exception : the stop method failed.
     * @see org.osgi.framework.BundleActivator#stop(org.osgi.framework.BundleContext)
     */
    public void stop(BundleContext bc) throws Exception {
        m_context.removeBundleListener(this);
        Enumeration e = m_components.keys();
        while (e.hasMoreElements()) {
            ComponentFactory[] cfs = (ComponentFactory[]) m_components.get(e.nextElement());
            for (int i = 0; i < cfs.length; i++) {
                cfs[i].stop();
            }
        }
        m_components = null;
        Enumeration e2 = m_creators.keys();
        while (e2.hasMoreElements()) {
            InstanceCreator creator = (InstanceCreator) m_creators.get(e2.nextElement());
            creator.stop();
        }
    }
    
    /**
     * Add a component factory to the factory list.
     * @param cm : the new component metadata.
     * @param bundle : the bundle.
     */
    private void addComponentFactory(Bundle bundle, Element cm) {        
        ComponentFactory factory = new ComponentFactory(bundle.getBundleContext(), cm);
        
        ComponentFactory[] cfs = (ComponentFactory[]) m_components.get(bundle);
        
        // If the factory array is not empty add the new factory at the end
        if (cfs != null && cfs.length != 0) {
            ComponentFactory[] newFactory = new ComponentFactory[cfs.length + 1];
            System.arraycopy(cfs, 0, newFactory, 0, cfs.length);
            newFactory[cfs.length] = factory;
            cfs = newFactory;
            m_components.put(bundle, cfs);
        } else {
            m_components.put(bundle, new ComponentFactory[] {factory}); // Else create an array of size one with the new Factory 
        }
    }
    
    /**
     * Start the management factories and create instances.
     * @param bundle : the bundle. 
     * @param confs : the instances to create.
     */
    private void start(Bundle bundle, Dictionary[] confs) {
        ComponentFactory[] cfs = (ComponentFactory[]) m_components.get(bundle);
        
        // Start the factories
        for (int j = 0; cfs != null && j < cfs.length; j++) {
            cfs[j].start();
        }

        Dictionary[] outsiders = new Dictionary[0];
        
        for (int i = 0; confs != null && i < confs.length; i++) {
            Dictionary conf = confs[i];
            boolean created = false;
            for (int j = 0; cfs != null && j < cfs.length; j++) {
                String componentClass = cfs[j].getComponentClassName();
                String factoryName = cfs[j].getName();
                String componentName = cfs[j].getComponentTypeName();
                if (conf.get("component") != null && (conf.get("component").equals(componentClass) || conf.get("component").equals(factoryName)) || conf.get("component").equals(componentName)) {
                    try {
                        cfs[j].createComponentInstance(conf);
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

        // Create the instance creator if needed.
        if (outsiders.length > 0) {
            m_creators.put(bundle, new InstanceCreator(bundle.getBundleContext(), outsiders));
        }
    }
    
    /**
     * Log an error message in a log service (if available) and display the message in the console.
     * @param message : the message to log
     * @param t : an attached error (can be null)
     */
    private void err(String message, Throwable t) {
        ServiceReference ref = m_context.getServiceReference(LogService.class.getName());
        if (ref != null) {
            LogService log = (LogService) m_context.getService(ref);
            log.log(LogService.LOG_ERROR, message, t);
            m_context.ungetService(ref);
        }
        if (t != null) {
            System.err.println("[iPOJO-Core] " + message + " : " + t.getMessage());
        } else {
            System.err.println("[iPOJO-Core] " + message);
        }
    }

}
